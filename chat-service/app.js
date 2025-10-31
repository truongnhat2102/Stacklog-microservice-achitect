// app.js
const express = require("express");
const dotenv = require("dotenv");
const cors = require("cors");
const helmet = require("helmet");
const morgan = require("morgan");
const http = require("http");

const connectDB = require("./config/db");
const { initProducer, initConsumer } = require("./config/kafka");

const boxesRoutes    = require("./routes/boxes");
const messagesRoutes = require("./routes/messages");

dotenv.config();
connectDB();

const app = express();

// Nếu đứng sau Nginx/Proxy, bật trust proxy để lấy IP thật & set cookie đúng
app.set("trust proxy", 1);

// CORS: nên dùng ENV để truyền nhiều origin, ví dụ CORS_ORIGINS=a,b,c
const CORS_ORIGINS = process.env.CORS_ORIGINS
  ? process.env.CORS_ORIGINS.split(",").map(s => s.trim())
  : [
      "http://localhost:5173",
      "https://stacklog.io.vn",
      "https://www.stacklog.io.vn",
      "https://*.vercel.app",
    ];

app.use(express.json());
app.use(
  cors({
    origin: CORS_ORIGINS,
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"],
    credentials: true,
  })
);
app.use(helmet());
app.use(morgan("dev"));

// Routes nội bộ (Nginx /api/chat/... sẽ strip prefix và đẩy vào đây)
app.use("/boxes", boxesRoutes);
app.use("/messages", messagesRoutes);

// Health check
app.get("/", (req, res) => {
  res.send("Chat Service is Running...");
});
app.get("/healthz", (req, res) => res.json({ ok: true }));

// Kafka: connect producer & consumer (đừng publish trống)
(async () => {
  try {
    await initProducer();
    await initConsumer();
    console.log("[kafka] producer/consumer ready");
  } catch (e) {
    console.error("[kafka] init error:", e);
    // Không nên process.exit ở đây để app vẫn phục vụ HTTP/WS nếu Kafka tạm chết
  }
})();

// Lắng nghe đúng cổng nội bộ khớp Nginx upstream (chatservice:2004)
// const PORT = process.env.PORT || 2004;
// server.listen(PORT, () => console.log(`Server running on port ${PORT}`));

// Graceful shutdown
process.on("unhandledRejection", (err) => {
  console.error("unhandledRejection:", err);
});
process.on("SIGINT", () => server.close(() => process.exit(0)));
process.on("SIGTERM", () => server.close(() => process.exit(0)));

module.exports = app;
