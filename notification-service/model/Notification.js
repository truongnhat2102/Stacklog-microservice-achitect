const { Schema, model } = require("mongoose");

const ReceiverSchema = new Schema({
  userId: { type: String, required: true },
  isRead: { type: Boolean, default: false },
  readAt: { type: Date }
});

const NotificationSchema = new Schema({
  content: { type: String, required: true },
  type: { type: String, enum: ["system", "chat", "task"], default: "system" },
  receivers: [ReceiverSchema],
  createdAt: { type: Date, default: Date.now },
  path: { type: String, required: false }
});

const Notification = model("Notification", NotificationSchema);

async function getAllNotifications() {
  return Notification.find().sort({ createdAt: -1 }).lean();
}

async function getNotificationsByUser(userId) {
  return Notification.find({ "receivers.userId": userId })
    .sort({ createdAt: -1 })
    .lean();
}

module.exports = {
  Notification,
  getAllNotifications,
  getNotificationsByUser,
};
