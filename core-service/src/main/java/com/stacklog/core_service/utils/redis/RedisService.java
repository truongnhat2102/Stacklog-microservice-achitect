package com.stacklog.core_service.utils.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacklog.core_service.utils.jwt.JwtDecoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisService<E> {

    private final Duration ttl = Duration.ofMinutes(600);

    private final JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Function<E, String> idExtractor;

    private final Class<E> clazz;

    public RedisService(Class<E> clazz, JwtDecoder jwtDecoder, Function<E, String> idExtractor) {
        this.clazz = clazz;
        this.jwtDecoder = jwtDecoder;
        this.idExtractor = idExtractor;
    }

    // ===== Create a key for E
    public String getKey(String currentUserId, String subtype, String nameService, String eId) {
        return nameService + ":" + clazz.getSimpleName() + ":" + currentUserId + ":" + subtype + ":" + eId;
    }

    // ===== method tạo indexKey đúng chuẩn =====
    private String getIndexKey(String currentUserId, String nameService) {
        return String.format("%s:%s:index", nameService, clazz.getSimpleName());
    }

    public String getCustomIndexKey(String currentUserId, String nameService, String suffix) {
        return String.format("%s:%s:index:%s", nameService, clazz.getSimpleName(), suffix);
    }

    // get and save to redis
    public List<E> getAll(String token, String nameService) {
        String currentUserId = getCurrentUserId(token);
        String indexKey = getIndexKey(currentUserId, nameService); // <-- dùng đúng key index
        Set<String> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys == null || keys.isEmpty())
            return Collections.emptyList();

        // Dùng multiGet để giảm round-trips
        List<String> keyList = new ArrayList<>(keys);
        List<String> jsons = redisTemplate.opsForValue().multiGet(keyList);

        List<E> results = new ArrayList<>();
        for (int i = 0; i < keyList.size(); i++) {
            String k = keyList.get(i);
            String json = (jsons != null && i < jsons.size()) ? jsons.get(i) : null;

            // Lazy cleanup: nếu item đã hết hạn -> gỡ khỏi index
            if (json == null) {
                redisTemplate.opsForSet().remove(indexKey, k);
                continue;
            }
            try {
                results.add(objectMapper.readValue(json, clazz));
            } catch (Exception ex) {
                log.warn("Failed to deserialize key {}: {}", k, ex.getMessage());
            }
        }

        // Gia hạn TTL cho index để không bị rò key mồ côi
        redisTemplate.expire(indexKey, ttl);
        return results;
    }

    public E getById(String eId, String token, String nameService) {
        String currentUserId = getCurrentUserId(token);
        String key = getKey(currentUserId, "web", nameService, eId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Deserialize failed: {}", e.getMessage());
            return null;
        }
    }

    public void saveListToRedis(List<E> list, String token, String nameService) {
        deleteAllByUserId(token, nameService);
        list.forEach(e -> {
            String id = idExtractor.apply(e);
            if (id != null) {
                saveToRedis(e, token, nameService);
            }
        });
    }

    public E saveToRedis(E e, String token, String nameService) {
        String currentUserId = getCurrentUserId(token);
        try {
            String json = objectMapper.writeValueAsString(e);
            String indexKey = getIndexKey(currentUserId, nameService);
            redisTemplate.opsForValue().set(getKey(currentUserId, "web", nameService, idExtractor.apply(e)), json, ttl);
            redisTemplate.opsForSet().add(indexKey, getKey(currentUserId, "web", nameService, idExtractor.apply(e)));
        } catch (JsonProcessingException e1) {
            log.error("❌ Failed to serialize object of type {}: {}", e.getClass().getName(), e1.getMessage());
        }
        return e;
    }

    public void deleteAllByUserId(String token, String nameService) {
        String currentUserId = getCurrentUserId(token);
        String indexKey = getIndexKey(currentUserId, nameService);
        Set<String> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null && !keys.isEmpty())
            redisTemplate.delete(keys);
        redisTemplate.delete(indexKey);
    }

    public List<E> getAllBySuffix(String token, String nameService, String suffix) {
        // pattern: task-service:Task:index:*:group:{groupId}
        String pattern = String.format("%s:%s:index:*:%s",
                nameService, clazz.getSimpleName(), suffix);

        Set<String> indexKeys = redisTemplate.keys(pattern);
        if (indexKeys == null || indexKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<E> results = new ArrayList<>();

        for (String indexKey : indexKeys) {
            Set<String> keys = redisTemplate.opsForSet().members(indexKey);
            if (keys == null || keys.isEmpty())
                continue;

            List<String> keyList = new ArrayList<>(keys);
            List<String> jsons = redisTemplate.opsForValue().multiGet(keyList);

            for (int i = 0; i < keyList.size(); i++) {
                String json = (jsons != null && i < jsons.size()) ? jsons.get(i) : null;
                if (json == null) {
                    redisTemplate.opsForSet().remove(indexKey, keyList.get(i));
                    continue;
                }
                try {
                    results.add(objectMapper.readValue(json, clazz));
                } catch (Exception ex) {
                    log.warn("❌ Deserialize fail for key {}: {}", keyList.get(i), ex.getMessage());
                }
            }

            // gia hạn TTL cho từng index
            redisTemplate.expire(indexKey, ttl);
        }

        return results;
    }

    public void saveListToRedisWithSuffix(List<E> list, String token, String nameService, String suffix) {
        String userId = getCurrentUserId(token);
        String indexKey = getCustomIndexKey(userId, nameService, suffix);
        // dọn index cũ rồi ghi lại
        Set<String> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null && !keys.isEmpty())
            redisTemplate.delete(keys);
        redisTemplate.delete(indexKey);
        list.forEach(e -> saveToRedisWithSuffix(e, token, nameService, suffix));
    }

    public void saveToRedisWithSuffix(E e, String token, String nameService, String suffix) {
        String userId = getCurrentUserId(token);
        String id = idExtractor.apply(e);
        if (id == null)
            return;
        try {
            String json = objectMapper.writeValueAsString(e);
            String itemKey = getKey(userId, "web", nameService, id);
            String indexKey = getCustomIndexKey(userId, nameService, suffix);
            redisTemplate.opsForValue().set(itemKey, json, ttl);
            redisTemplate.opsForSet().add(indexKey, itemKey);
            redisTemplate.expire(indexKey, ttl);
        } catch (Exception ex) {
            log.warn("Serialize fail: {}", ex.getMessage());
        }
    }

    public String getCurrentUserId(String token) {
        token = normalizeBearer(token); // không split mù, tránh NPE
        String userId = jwtDecoder.getIdFromToken(token);

        String device = "web"; // nếu có đa thiết bị: truyền vào param/claim
        String key = "auth:session:" + userId + ":" + device;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !token.equals(storedToken)) {
            throw new RuntimeException("Token invalid or expired");
        }
        return userId;
    }

    public String getCurrentRoleId(String token) {
        token = normalizeBearer(token); // không split mù, tránh NPE
        String userRole = jwtDecoder.getRoleFromToken(token);
        String userId = jwtDecoder.getIdFromToken(token);
        String device = "web"; // nếu có đa thiết bị: truyền vào param/claim
        String key = "auth:session:" + userId + ":" + device;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !token.equals(storedToken)) {
            throw new RuntimeException("Token invalid or expired");
        }
        return userRole;
    }

    private String normalizeBearer(String token) {
        if (token == null)
            throw new RuntimeException("Missing token");
        String t = token.trim();
        if (t.regionMatches(true, 0, "Bearer ", 0, 7))
            t = t.substring(7).trim();
        if (t.isEmpty())
            throw new RuntimeException("Invalid token");
        return t;
    }
}
