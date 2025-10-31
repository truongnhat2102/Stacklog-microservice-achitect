const { Server } = require('socket.io');

// OPTIONAL for scale-out (nhiều instance sau Nginx)
let createAdapter, createClient;
try {
  ({ createAdapter } = require('@socket.io/redis-adapter'));
  ({ createClient } = require('redis'));
} catch (_) {
  // không bắt buộc, chỉ dùng khi cài @socket.io/redis-adapter và redis
}

let io = null;

/**
 * Attach Socket.IO vào HTTP server nội bộ của chat-service.
 * Phù hợp khi đứng sau Nginx reverse proxy (đã bật Upgrade/Connection).
 */
async function attachSocket(server) {
  console.log('[socket] attaching...');
  io = new Server(server, {
    path: '/socket.io/',
    origin: [
      'http://localhost:5173',
      'https://stacklog.io.vn',
      'https://www.stacklog.io.vn',
      'https://*.vercel.app'
    ],
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true
  });
  console.log('[socket] attached OK at /socket.io');
  io.on('connection', (socket) => {
    const userId = socket.handshake.query?.userId || socket.handshake.auth?.userId;
    if (userId) {
      socket.join(`user:${userId}`);
    }

    // Tham gia/thoát room theo box
    socket.on('room:join', (boxId) => {
      if (!boxId) return;
      socket.join(`box:${boxId}`);
    });

    socket.on('room:leave', (boxId) => {
      if (!boxId) return;
      socket.leave(`box:${boxId}`);
    });
  });

  // Gợi ý graceful shutdown: đóng io khi process thoát
  const shutdown = () => {
    if (io) {
      io.close(() => console.log('[socket] closed'));
    }
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  console.log(`[socket] attached at path=/socket.io cors='http://localhost:5173',
      'https://stacklog.io.vn',
      'https://www.stacklog.io.vn',
      'https://*.vercel.app'`);
}

/**
 * Emit tiện lợi: phát ra toàn hệ thống hoặc phòng cụ thể (string hoặc string[])
 */
function ioEmit(event, payload, room) {
  if (!io) {
    console.warn('[socket] io not initialized; skip emit');
    return;
  }
  if (!room) {
    io.emit(event, payload);
    return;
  }
  if (Array.isArray(room)) {
    room.forEach(r => io.to(r).emit(event, payload));
  } else {
    io.to(room).emit(event, payload);
  }
}

function getIO() {
  return io;
}

module.exports = { attachSocket, ioEmit, getIO };
