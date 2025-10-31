// models/box.js
const { Schema, model } = require('mongoose');
const { v4: uuidv4 } = require('uuid');

const MemberSchema = new Schema({
  isAdmin: { type: Boolean, default: false },
  isMute: { type: Boolean, default: false },
  joinedAt: { type: Date, default: Date.now },
  addedBy: { type: String, default: null },
  userId: { type: String, default: null }
}, { _id: false });

const BoxChatSchema = new Schema({
  _id: { type: String, required: true },          // box_chat_id (uuid hoặc groupId)
  name_box: { type: String },
  ava_box: { type: String },
  created_by: { type: String },
  boxType: { type: String, enum: ['PERSONAL', 'GROUP'], default: 'GROUP', index: true },
  members: { type: [MemberSchema], default: [] }
}, { timestamps: { createdAt: 'created_at', updatedAt: 'updated_at' }, versionKey: false });

BoxChatSchema.virtual('box_chat_id').get(function () { return this._id; });
BoxChatSchema.index({ 'members.user': 1 });
BoxChatSchema.index({ updated_at: -1 });

const BoxChat = model('BoxChat', BoxChatSchema);

// Services
async function createBox({ name, avatar, creatorId, memberIds, type }) {
  const id = uuidv4();
  const set = new Set(memberIds || []);
  set.add(String(creatorId)); // đảm bảo có creator
  const members = Array.from(set).map(uid => ({
    userId: uid, isAdmin: String(uid) === String(creatorId), addedBy: creatorId
  }));
  const doc = await BoxChat.create({ _id: id, name_box: name, ava_box: avatar, created_by: creatorId, members, boxType: type });
  return doc.toObject();
}

// Dùng $addToSet + $each để tránh race condition & code gọn
async function addMembers(boxId, operatorId, memberIds = []) {
  const uniqueIds = Array.from(new Set(memberIds));
  if (!uniqueIds.length) return;
  await BoxChat.updateOne(
    { _id: boxId },
    {
      $addToSet: {
        members: {
          $each: uniqueIds.map(uid => ({ userId: uid, isAdmin: false, isMute: false, addedBy: operatorId }))
        }
      }
    }
  );
}

async function listBoxesByUser(userId) {
  return BoxChat.find({ 'members.userId': userId })
    .sort({ updated_at: -1 }) // dùng updated_at cho thống nhất
    .select({
      _id: 1, name_box: 1, ava_box: 1, boxType: 1, updated_at: 1, members: 1, created_at: 1, updated_at: 1,
    })
    .lean();
}

async function autoCreateBoxFromGroupEvent({ groupId, name, avatar, memberIds = [], createdBy }) {
  await BoxChat.updateOne(
    { _id: groupId },
    { $setOnInsert: { name_box: name, ava_box: avatar, created_by: createdBy, members: [] } },
    { upsert: true }
  );

  const set = new Set(memberIds);
  set.add(String(createdBy)); // bảo đảm creator cũng là member
  const uniqueIds = Array.from(set);

  await BoxChat.updateOne(
    { _id: groupId },
    {
      $addToSet: {
        members: { $each: uniqueIds.map(uid => ({ userId: uid, isAdmin: false, isMute: false, addedBy: createdBy })) }
      }
    }
  );

  return BoxChat.findById(groupId).lean();
}

async function deleteBox(boxId) {
  return BoxChat.deleteOne({ _id: boxId });
}

async function searchBoxesByUserIds(userIds = []) {
  if (!Array.isArray(userIds) || userIds.length === 0) return [];

  return BoxChat.find({
    'members.userId': { $all: userIds },
    $expr: { $eq: [{ $size: "$members" }, userIds.length] }
  })
    .sort({ updated_at: -1 })
    .select({
      _id: 1,
      name_box: 1,
      ava_box: 1,
      boxType: 1,
      members: 1,
      created_at: 1,
      updated_at: 1
    })
    .lean();
}

async function deleteMemberSchema(userId, boxId) {
  if (!userId || !boxId) throw new Error("userId and boxId are required");

  // Xóa member khỏi danh sách members trong BoxChat
  const result = await BoxChat.updateOne(
    { _id: boxId },
    { $pull: { members: { userId: userId } } }
  );

  return result;
}

module.exports = { BoxChat, createBox, addMembers, listBoxesByUser, autoCreateBoxFromGroupEvent, deleteBox, searchBoxesByUserIds, deleteMemberSchema };