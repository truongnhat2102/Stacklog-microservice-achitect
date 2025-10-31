const { Kafka } = require('kafkajs');
const mongoose = require('mongoose');
const User = require('../models/User'); 
const { extractWorkId } = require('../utils/helperMethod');

const kafka = new Kafka({
  clientId: process.env.KAFKA_CLIENT_ID || 'profile-service',
  brokers: [process.env.KAFKA_BROKER],
});

const producer = kafka.producer();
const consumerProfileService = kafka.consumer({ groupId: 'profile-service-group' });

/**
 * Gửi message lên Kafka topic
 */
const produceMessage = async (topic, message) => {
  await producer.connect();
  await producer.send({
    topic,
    messages: [{ value: JSON.stringify(message) }],
  });
  console.log(`✅ [Kafka] Message sent to ${topic}:`, message);
};

/**
 * Khởi động consumer để lắng nghe event từ các service khác
 */
const initConsumer = async () => {
  await consumerProfileService.connect();
  console.log('✅ Kafka Consumer connected: profile-service');

  // Đăng ký các topic cần lắng nghe
  await consumerProfileService.subscribe({
    topics: ['auth-service.user.created'], // topic từ auth-service
    fromBeginning: false,
  });

  await consumerProfileService.run({
    eachMessage: async ({ topic, partition, message }) => {
      try {
        const data = JSON.parse(message.value.toString());
        console.log(`📩 [Kafka] Received Event: ${topic}`, data);

        if (topic === 'auth-service.user.created') {
          await handleUserCreated(data);
        }
      } catch (error) {
        console.error('❌ Error processing Kafka message:', error);
      }
    },
  });
};

/**
 * Xử lý sự kiện user được tạo từ auth-service
 */
const handleUserCreated = async (data) => {
  try {
    const existingUser = await User.findOne({ email: data.email, isDeleted: false });

    if (existingUser) {
      console.log(`⚠️ User with email ${data.email} already exists.`);
      return;
    }

    const newUser = new User({
      _id: data.id,
      full_name: data.name,
      email: data.email,
      avatar_link: data.avatar || null,
      role: data.role || 'STUDENT',
      work_id: extractWorkId(data.email),
    });

    await newUser.save();
    console.log(`✅ [Profile-Service] Created new user: ${data.email}`);

  } catch (err) {
    console.error('❌ Error creating user from Kafka event:', err);
  }
};

module.exports = { produceMessage, initConsumer };
