const { createClient } = require("redis");

// === Khởi tạo client ===
const redisClient = createClient({
  socket: {
    host: process.env.REDIS_HOST || "127.0.0.1",
    port: process.env.REDIS_PORT || 6379,
  },
});

redisClient.on("error", (err) => {
  console.error("❌ Redis Error:", err);
});

redisClient.connect().then(() => {
  console.log("✅ Redis connected successfully");
});

// === RedisService Class ===
class RedisService {
  constructor({ jwtDecoder, idExtractor, entityName, ttlMinutes = 600 }) {
    if (!jwtDecoder?.getIdFromToken) throw new Error("jwtDecoder.getIdFromToken required");
    if (!idExtractor) throw new Error("idExtractor required");
    if (!entityName) throw new Error("entityName required");

    this.redis = redisClient; // <-- dùng client global ở trên
    this.jwtDecoder = jwtDecoder;
    this.idExtractor = idExtractor;
    this.entityName = entityName;
    this.ttlSeconds = Math.max(1, Math.floor(ttlMinutes * 60));
  }

  getKey(currentUserId, subtype, nameService, eId) {
    return `${nameService}:${this.entityName}:${currentUserId}:${subtype}:${eId}`;
  }
  getIndexKey(currentUserId, nameService) {
    return `${nameService}:${this.entityName}:index:${currentUserId}`;
  }
  getCustomIndexKey(currentUserId, nameService, suffix) {
    return `${nameService}:${this.entityName}:index:${currentUserId}:${suffix}`;
  }

  async getAll(token, nameService) {
    const currentUserId = await this.getCurrentUserId(token);
    const indexKey = this.getIndexKey(currentUserId, nameService);
    const keys = await this.redis.sMembers(indexKey);
    if (!keys?.length) return [];

    const jsons = await this.redis.mGet(keys);
    const results = [];

    for (let i = 0; i < keys.length; i++) {
      const k = keys[i];
      const json = jsons?.[i] ?? null;
      if (json == null) {
        await this.redis.sRem(indexKey, k);
        continue;
      }
      try {
        results.push(JSON.parse(json));
      } catch {
        await this.redis.sRem(indexKey, k);
      }
    }

    await this.redis.expire(indexKey, this.ttlSeconds);
    return results;
  }

  async getById(eId, token, nameService) {
    const currentUserId = await this.getCurrentUserId(token);
    const key = this.getKey(currentUserId, "web", nameService, eId);
    const json = await this.redis.get(key);
    return json ? JSON.parse(json) : null;
  }

  async saveToRedis(e, token, nameService) {
    const currentUserId = await this.getCurrentUserId(token);
    const id = this.idExtractor(e);
    if (!id) return e;

    const json = JSON.stringify(e);
    const itemKey = this.getKey(currentUserId, "web", nameService, id);
    const indexKey = this.getIndexKey(currentUserId, nameService);

    await this.redis.set(itemKey, json, { EX: this.ttlSeconds });
    await this.redis.sAdd(indexKey, itemKey);
    await this.redis.expire(indexKey, this.ttlSeconds);
    return e;
  }

  async deleteAllByUserId(token, nameService) {
    const currentUserId = await this.getCurrentUserId(token);
    const indexKey = this.getIndexKey(currentUserId, nameService);
    const keys = await this.redis.sMembers(indexKey);
    if (keys?.length) await this.redis.del(keys);
    await this.redis.del(indexKey);
  }

  async getCurrentUserId(token) {
    const t = this.normalizeBearer(token);
    const userId = this.jwtDecoder.getIdFromToken(t);
    const key = `auth:session:${userId}:web`;
    const storedToken = await this.redis.get(key);
    if (!storedToken || storedToken !== t) {
      throw new Error("Token invalid or expired");
    }
    return userId;
  }

  async getCurrentRoleId(token) {
    const t = this.normalizeBearer(token);
    const role = this.jwtDecoder.getRoleFromToken(t);
    const userId = this.jwtDecoder.getIdFromToken(t);
    const key = `auth:session:${userId}:web`;
    const storedToken = await this.redis.get(key);
    if (!storedToken || storedToken !== t) {
      throw new Error("Token invalid or expired");
    }
    return role;
  }

  normalizeBearer(token) {
    if (!token) throw new Error("Missing token");
    let t = String(token).trim();
    if (t.toLowerCase().startsWith("bearer ")) t = t.slice(7).trim();
    if (!t) throw new Error("Invalid token");
    return t;
  }
}

// === Export cả client và class ===
module.exports = {
  redisClient,
  RedisService,
};
