const asyncHandler = require('../middleware/asyncHandler');
const { createBox, addMembers, listBoxesByUser, deleteBox, searchBoxesByUserIds, deleteMemberSchema, BoxChat } = require('../models/box');
const { deleteByBoxId } = require('../models/message');
const { sendKafkaEvent } = require('../config/kafka');
const { ioEmit } = require('../config/socket');

const TOPIC_CHATBOX_KICKED = process.env.TOPIC_MESSAGE_CREATED || 'chat-service.boxchat.kicked';

exports.create = asyncHandler(async (req, res) => {
  const { name, avatar, memberIds = [], type } = req.body || {};
  const creatorId = req.user.id;  // <-- lấy từ token
  if (!memberIds.length) return res.status(400).json({ message: 'memberIds required' });

  let box = await searchBoxesByUserIds(memberIds);

  if (box.length > 0) return res.status(200).json(box);

  box = await createBox({ name, avatar, creatorId, memberIds, type });
  for (const uid of new Set(memberIds)) {
    ioEmit('box:created', { box_chat_id: box.box_chat_id, name_box: box.name_box }, `user:${uid}`);
  }
  res.status(201).json(box);
});

exports.addMembers = asyncHandler(async (req, res) => {
  const { boxId } = req.params;
  const operatorId = req.user.id;  // <-- lấy từ token
  const { memberIds = [] } = req.body || {};
  await addMembers(boxId, operatorId, memberIds);
  for (const uid of new Set(memberIds)) {
    ioEmit('box:member_added', { box_chat_id: boxId, user_id: uid }, `user:${uid}`);
  }
  res.status(204).end();
});

exports.listByUser = asyncHandler(async (req, res) => {
  const userId = req.user.id;   // <-- lấy từ token
  const boxes = await listBoxesByUser(userId);
  res.json(boxes);
});

exports.delete = asyncHandler(async (req, res) => {
  try {
    const { boxId } = req.params;
    if (!boxId) {
      return res.status(400).json({ success: false, error: "boxId is required" });
    }

    await deleteByBoxId(boxId);

    const result = await deleteBox(boxId);


    // Emit socket để client update UI
    ioEmit("box:deleted", { boxId, ...result }, `box:${boxId}`);

    res.json({ success: true, message: "Box deleted", result });
  } catch (err) {
    // Ghi log chi tiết
    console.error(`[BoxController] Delete boxId=${req.params.boxId} error:`, err);

    res.status(500).json({
      success: false,
      error: err.message || "Internal Server Error",
    });
  }
});

exports.deleteMember = asyncHandler(async (req, res) => {
  try {
    const { boxId, memberId } = req.params;

    // 1️⃣ Kiểm tra đầu vào
    if (!boxId || !memberId) {
      return res.status(400).json({
        success: false,
        error: "Both boxId and memberId are required",
      });
    }

    // 2️⃣ Xóa member khỏi box
    const result = await deleteMemberSchema(memberId, boxId);

    const payload = { userId: memberId, box: await BoxChat.findOne({ _id: boxId }) }

    // 3️⃣ Emit socket cho client cập nhật UI (xóa member trong box)
    ioEmit("member:deleted", { boxId, memberId }, `box:${boxId}`);

    await sendKafkaEvent(TOPIC_CHATBOX_KICKED, payload);

    // 4️⃣ Trả phản hồi cho client
    return res.json({
      success: true,
      message: "Member removed successfully",
      result,
    });
  } catch (err) {
    console.error(`[BoxController] Delete member from boxId=${req.params.boxId} error:`, err);
    return res.status(500).json({
      success: false,
      error: err.message || "Internal Server Error",
    });
  }
});