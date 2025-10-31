const nodemailer = require("nodemailer");
require("dotenv").config();

// Tạo một transporter với thông tin SMTP của bạn
async function sendEmail(from, to, subject, text, attachments = []) {
  try {
    let transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: process.env.EMAIL_STACKLOG_USER,
        pass: process.env.EMAIL_STACKLOG_PASSWORD
      }
    });

    // Định nghĩa email bạn muốn gửi
    let mailOptions = {
      from: process.env.EMAIL_STACKLOG_USER,
      to: to,
      subject: subject,
      text: text,
      attachments: attachments
    };

    // Gửi email
    const info = await transporter.sendMail(mailOptions);
    console.log('Email sent: ' + info.response);
  } catch (error) {
    console.error('Error sending email:', error);
  }
}

module.exports = { sendEmail };
