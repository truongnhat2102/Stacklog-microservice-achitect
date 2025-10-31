// models/message.js
const { Schema, model } = require('mongoose');
const { v4: uuidv4 } = require('uuid');

const ChatMessageSchema = new Schema({
  _id:                      { type: String, required: true }, // chat_message_id
  box_chat_id:              { type: String, required: true, index: true },
  created_by:               { type: String, required: true, index: true },
  chat_message_content:     { type: String, default: null },
  chat_message_attachment:  { type: [Schema.Types.Mixed], default: [] },
  state:                    { type: String, enum: ['SENT','RECALLED','DELETED'], default: 'SENT', index: true },
  deleted_at:               { type: Date, default: null },
  read_by:                  { type: [String], default: [] }
}, { timestamps: { createdAt: 'created_at', updatedAt: 'update_at' }, versionKey: false });

ChatMessageSchema.virtual('chat_message_id').get(function(){ return this._id; });
ChatMessageSchema.index({ box_chat_id: 1, created_at: -1 });

const ChatMessage = model('ChatMessage', ChatMessageSchema);

async function insertMessage({ boxId, senderId, content, attachment = null }) {
  const id = uuidv4();
  await ChatMessage.create({
    _id: id,
    box_chat_id: boxId,
    created_by: senderId,
    chat_message_content: content || null,
    chat_message_attachment: attachment ? [attachment] : []
  });
  return id;
}

async function listMessages(boxId, { limit = 50, beforeMessageId = null }) {
  const q = { box_chat_id: boxId };
  if (beforeMessageId) {
    const pivot = await ChatMessage.findById(beforeMessageId).select({ created_at: 1 }).lean();
    if (pivot) q.created_at = { $lt: pivot.created_at };
  }
  return ChatMessage.find(q).sort({ created_at: -1 }).limit(Number(limit)).lean().then(arr => arr.reverse());
}

async function recallMessage(messageId, operatorId) {
  await ChatMessage.updateOne(
    { _id: messageId, state: 'SENT' },
    { $set: { state: 'RECALLED', chat_message_content: null, chat_message_attachment: [], update_by: operatorId, update_at: new Date() } }
  );
}

async function softDeleteMessage(messageId, operatorId) {
  await ChatMessage.updateOne(
    { _id: messageId },
    { $set: { state: 'DELETED', deleted_at: new Date(), update_by: operatorId, update_at: new Date() } }
  );
}

async function hardDeleteMessage(messageId) {
  await ChatMessage.deleteOne({ _id: messageId });
}

async function deleteByBoxId(boxId) {
  const deletedMessages = await ChatMessage.deleteMany({ box_chat_id: boxId });

  return {
    deletedMessageCount: deletedMessages.deletedCount
  };
}

module.exports = {
  ChatMessage,
  insertMessage, listMessages,
  recallMessage, softDeleteMessage, hardDeleteMessage, deleteByBoxId
};
