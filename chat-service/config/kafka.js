const { Kafka } = require("kafkajs");
require("dotenv").config();


const { autoCreateBoxFromGroupEvent } = require('../models/box');
const { ioEmit } = require('./socket');

const KAFKA_BROKER = process.env.KAFKA_BROKER || "localhost:9092";
const CLIENT_ID = process.env.KAFKA_CLIENT_ID || "chat-service";
const GROUP_ID = process.env.KAFKA_GROUP_ID || "chat-group";

// Khởi tạo Kafka Client
const kafka = new Kafka({
  clientId: CLIENT_ID,
  brokers: [KAFKA_BROKER],
});

// Khởi tạo Producer
const producer = kafka.producer();
const consumer = kafka.consumer({ groupId: GROUP_ID });

// Khởi động Producer
const initProducer = async () => {
  await producer.connect();
  console.log("Kafka Producer is ready");
};

// Khởi động Consumer
const initConsumer = async () => {
  await consumer.connect();
  console.log("Kafka Consumer is ready");

  const topicGroupCreated =
    process.env.TOPIC_GROUP_CREATED || "class-service.groupsses.created";

  // chỉ subscribe một topic và không đọc lại từ đầu
  await consumer.subscribe({ topic: topicGroupCreated, fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ topic, message }) => {
      // bỏ qua nếu không đúng topic (phòng khi consumer được dùng chung)
      if (topic !== topicGroupCreated) return;

      try {
        const payload = JSON.parse(message.value?.toString() || "{}");

        // Tự tạo box từ sự kiện group
        const box = await autoCreateBoxFromGroupEvent(payload);

        // Phát socket tới từng member
        const memberIds = Array.isArray(payload.memberIds) ? payload.memberIds : [];
        for (const uid of memberIds) {
          ioEmit(
            "box:created",
            { box_chat_id: box.box_chat_id, name_box: box.name_box },
            `user:${uid}`
          );
        }
      } catch (e) {
        console.error("Kafka consume error:", e);
      }
    },
  });
};

const sendKafkaEvent = async (topic, value) => {
  try {
    await producer.send({
      topic,
      messages: [{ value: JSON.stringify(value) }],
    });
    console.log(`Kafka Event Sent: ${topic}`);
  } catch (err) {
    console.error("Kafka Send Error:", err);
  }
};



module.exports = { initProducer, initConsumer, sendKafkaEvent };










