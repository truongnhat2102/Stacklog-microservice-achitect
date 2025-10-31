// controllers/messages.controller.js
const asyncHandler = require('../middleware/asyncHandler');
const { redisClient } = require('../config/redis');  // <-- đổi tên
const { ioEmit } = require('../config/socket');
const { sendKafkaEvent } = require('../config/kafka');
const {
  ChatMessage,
  insertMessage, listMessages, recallMessage,
  softDeleteMessage, hardDeleteMessage
} = require('../models/message');
const { BoxChat } = require('../models/box');

const TOPIC_MESSAGE_CREATED = process.env.TOPIC_MESSAGE_CREATED || 'chat-service.message.created';
const TOPIC_MESSAGE_MENTION = process.env.TOPIC_MESSAGE_MENTION || 'chat-service.message.mention';
const unreadKey = (boxId, userId) => `unread:${boxId}:${userId}`;
const lastKey = (boxId) => `lastmsg:${boxId}`;

// ==== List messages ====
exports.list = asyncHandler(async (req, res) => {
  const { boxId } = req.params;
  const { limit, beforeMessageId } = req.query;
  const msgs = await listMessages(boxId, { limit, beforeMessageId });
  res.json(msgs);
});

// ==== Send message ====
exports.send = asyncHandler(async (req, res) => {
  const { boxId } = req.params;
  const senderId = req.user.id;  // <-- lấy từ middleware auth
  const { content, attachment, mentionUserIds = [] } = req.body || {};

  if (!senderId) return res.status(400).json({ message: 'senderId required' });

  const box = await BoxChat.findById(boxId).select({ members: 1 }).lean();
  if (!box) return res.status(404).json({ message: 'Box not found' });

  const isMember = (box.members || []).some(m => String(m.userId) === String(senderId));
  if (!isMember) return res.status(403).json({ message: 'Forbidden' });

  const msgId = await insertMessage({ boxId, senderId, content, attachment });

  // save last message cache
  await redisClient.set(
    lastKey(boxId),
    JSON.stringify({ chat_message_id: msgId, preview: content, at: Date.now() }),
    { EX: 3600 }
  );

  // update box updated_at
  await BoxChat.updateOne({ _id: boxId }, { $set: { updated_at: new Date() } });

  // unread++ cho mọi member trừ sender
  for (const m of box.members || []) {
    if (String(m.userId) === String(senderId)) continue;
    await redisClient.incr(unreadKey(boxId, m.userId));
  }

  const payload = { chat_message_id: msgId, box_chat_id: boxId, sender_id: senderId, content, attachment, state: 'SENT', mentionUserIds: mentionUserIds };
  ioEmit('message:new', payload, `box:${boxId}`);
  for (const uid of new Set(mentionUserIds)) {
    ioEmit('notify:mention', { box_chat_id: boxId, chat_message_id: msgId }, `user:${uid}`);
  }

  await sendKafkaEvent(TOPIC_MESSAGE_CREATED, payload);
  await sendKafkaEvent(TOPIC_MESSAGE_MENTION, payload);
  res.status(201).json(payload);
});

// ==== Recall message ====
exports.recall = asyncHandler(async (req, res) => {
  const { messageId } = req.params;
  const operatorId = req.user.id;

  const msg = await ChatMessage.findById(messageId).lean();
  if (!msg) return res.status(404).json({ message: 'Message not found' });

  await recallMessage(messageId, operatorId);
  ioEmit('message:recalled', { chat_message_id: messageId }, `box:${msg.box_chat_id}`);
  res.status(204).end();
});

// ==== Delete message ====
exports.remove = asyncHandler(async (req, res) => {
  const { messageId } = req.params;
  const operatorId = req.user.id;
  const hard = String(req.query.hard || '0') === '1';

  const msg = await ChatMessage.findById(messageId).lean();
  if (!msg) return res.status(404).json({ message: 'Message not found' });

  if (hard) await hardDeleteMessage(messageId);
  else await softDeleteMessage(messageId, operatorId);

  ioEmit('message:deleted', { chat_message_id: messageId, hard }, `box:${msg.box_chat_id}`);
  res.status(204).end();
});

// ==== Reset unread ====
exports.readReset = asyncHandler(async (req, res) => {
  const { boxId } = req.params;
  const userId = req.user.id;
  await redisClient.del(unreadKey(boxId, userId));
  res.status(204).end();
});

exports.markAsRead = asyncHandler(async (req, res) => {
  const { boxId } = req.params;
  const userId = String(req.user.id);

  // lấy tất cả messages chưa có userId trong readBy
  const messages = await ChatMessage.find({
    box_chat_id: boxId,
    read_by: { $ne: userId }
  }).select('_id');

  if (messages.length == 0) return res.status(202).json({ success: true });

  // update messages thêm userId vào readBy
  await ChatMessage.updateMany(
    { _id: { $in: messages.map(m => m._id) } },
    { $push: { read_by: userId } }
  );

  // reset unread count trong Redis
  await redisClient.set(unreadKey(boxId, userId), 0);

  // emit socket để các client khác update "đã xem"
  ioEmit('message:read', { 
    box_chat_id: boxId, 
    user_id: userId,
    last_read_message_id: messages[messages.length - 1]._id
  }, `box:${boxId}`);

  res.status(200).json({ success: true });
});
