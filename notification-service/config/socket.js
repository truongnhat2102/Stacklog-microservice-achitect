const { Server } = require("socket.io");

let io = null;

/**
 * Attach Socket.IO vào HTTP server của notification-service.
 */
async function attachSocket(server) {
  console.log("[socket] attaching (notification)...");
  io = new Server(server, {
    path: "/socket.io/",
    cors: {
      origin: [
        "http://localhost:5173",
        "https://stacklog.io.vn",
        "https://www.stacklog.io.vn",
        /\.vercel\.app$/, // regex cho *.vercel.app
      ],
      methods: ["GET", "POST", "OPTIONS"],
      allowedHeaders: ["Content-Type", "Authorization"],
      credentials: true,
    },
  });

  console.log("[socket] attached OK at /socket.io (notification)");

  io.on("connection", (socket) => {
    const userId =
      socket.handshake.query?.userId || socket.handshake.auth?.userId;

    if (userId) {
      socket.join(`user:${userId}`);
      console.log(`[socket] user ${userId} connected, joined room user:${userId}`);
    } else {
      console.warn("[socket] connection without userId");
    }

    socket.on("disconnect", () => {
      console.log("[socket] client disconnected:", userId);
    });
  });

  // Graceful shutdown
  const shutdown = () => {
    if (io) {
      io.close(() => console.log("[socket] closed (notification)"));
    }
  };
  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

/**
 * Emit tiện lợi: phát notification cho user cụ thể hoặc broadcast.
 */
function ioEmitNotification(payload, userIds) {
  if (!io) {
    console.warn("[socket] io not initialized; skip emit");
    return;
  }

  if (!userIds) {
    io.emit("notification", payload); // broadcast all
    return;
  }

  if (Array.isArray(userIds)) {
    userIds.forEach((id) =>{
      io.to(`user:${id}`).emit("notification", payload)
    });
  } else {
    io.to(`user:${userIds}`).emit("notification", payload);
  }
}

function getIO() {
  return io;
}

module.exports = { attachSocket, ioEmitNotification, getIO };
