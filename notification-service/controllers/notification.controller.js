const {
    Notification,
    getAllNotifications,
    getNotificationsByUser,
} = require("../model/Notification");

const { redisService } = require("../config/redis");
const { ioEmitNotification } = require('../config/socket');
const { sendEmail } = require("../config/email");
const { getStudentEmails } = require("../helper/api.service");

/**
 * GET /api/notifications (tất cả hệ thống - chỉ admin mới nên dùng)
 */
async function listAll(req, res) {
    try {
        // Có thể kiểm tra role nếu cần
        if (req.user?.role !== "admin") {
            return res.status(403).json({ success: false, error: "Forbidden" });
        }

        const notifications = await getAllNotifications();
        res.json(notifications);
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
}

/**
 * GET /api/notifications/me (lấy theo user từ token)
 */
async function listByUser(req, res) {
    try {
        const userId = req.user?.id;
        if (!userId) {
            return res.status(401).json({ success: false, error: "Unauthorized" });
        }

        const notifications = await getNotificationsByUser(userId);
        res.json(notifications);
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
}

async function createNotification(userIds, content, type = "system", meta = {}, path) {
    if (!Array.isArray(userIds) || userIds.length === 0) {
        throw new Error("userIds required");
    }

    // 1. Lưu Mongo
    const notification = await Notification.create({
        content,
        type,
        receivers: userIds.map((uid) => ({ userId: uid })),
        path,
        meta,

    });

    // 2. Lưu Redis + emit socket realtime
    for (const uid of userIds) {
        // await redisService.saveToRedis(notification.toObject(), null, "notification-service");
        ioEmitNotification(notification, `${uid}`);
        console.log(`noti:${notification} user:${uid}`);
    }

    return notification;
}

async function sendNotification(req, res) {
    try {
        const classIds = req.body.listClassId;
        const authHeader = req.headers['authorization'];
        const token = authHeader.split(' ')[1];
        // Gọi API để lấy danh sách học viên từ các nhóm thuộc các classIds
        const emails = await getStudentEmails(classIds, token); // Giả sử hàm này sẽ trả về danh sách email của học viên trong lớp


        // Kiểm tra nếu không có email nào để gửi
        if (emails.length === 0) {
            return res.status(400).json({ message: 'No students found to send notification.' });
        }

        // Tiến hành gửi email cho tất cả học viên
        const subject = req.body.subject;  // Tiêu đề email
        const text = req.body.content;  // Nội dung email

        for (const email of emails) {
            await sendEmail(
                "chumlu2102@gmail.com",
                email,                   // Địa chỉ email người nhận
                subject,                 // Tiêu đề
                text                     // Nội dung email
            );
        }

        // Trả về kết quả thành công
        res.status(200).json({ message: 'Emails sent successfully' });

    } catch (err) {
        console.error('Error sending notifications:', err);
        res.status(500).json({ message: 'Send mail failed' });
    }
}

module.exports = {
    listAll,
    listByUser,
    createNotification,
    sendNotification
};
