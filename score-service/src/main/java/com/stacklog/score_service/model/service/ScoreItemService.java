package com.stacklog.score_service.model.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stacklog.core_service.model.service.IService;
import com.stacklog.core_service.utils.CommonFunction;
import com.stacklog.core_service.utils.kafka.KafkaProducer;
import com.stacklog.core_service.utils.redis.RedisService;
import com.stacklog.score_service.model.entities.ScoreItem;
import com.stacklog.score_service.model.repo.ScoreItemRepo;

import jakarta.transaction.Transactional;

@Service
public class ScoreItemService implements IService<ScoreItem> {

    private static final String NAME_SERVICE = "score-service";

    private static final String KAFKA_TOPIC_UPDATE = "score-service.scoreitem.updated";
    private static final String KAFKA_TOPIC_CREATE = "score-service.scoreitem.created";

    @Autowired
    private ScoreItemRepo scoreItemRepo;

    @Autowired
    private ClassServiceClient classServiceClient;

    @Autowired
    private KafkaProducer<ScoreItem> kafkaScoreItemProducer;

    private final RedisService<ScoreItem> redisScoreItemService;

    public ScoreItemService(RedisService<ScoreItem> redisScoreItemService) {
        this.redisScoreItemService = redisScoreItemService;
    }

    @Override
    public List<ScoreItem> getAllByUserId(String token) {
        List<ScoreItem> lists = redisScoreItemService.getAll(token, NAME_SERVICE);
        if (lists == null || lists.isEmpty()) {
            String userId = redisScoreItemService.getCurrentUserId(token);
            lists = scoreItemRepo.findAllByUserId(userId);
            redisScoreItemService.saveListToRedis(lists, token, NAME_SERVICE);
        }
        return lists;
    }

    public List<ScoreItem> getAllByGroupId(String token, String groupId) {
        List<String> userIds = classServiceClient.getGroupStudent(token, groupId)
                .stream()
                .map(GroupStudent::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (userIds.isEmpty())
            return List.of();

        List<ScoreItem> scoreItems = scoreItemRepo.findScoreItemByUserIds(groupId, userIds);

        redisScoreItemService.saveListToRedis(scoreItems, token, "score-service");
        return scoreItems;
    }

    @Override
    public ScoreItem getById(String id, String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public ScoreItem save(ScoreItem e, String token) {
        boolean isCreate = (e.getScoreItemId() == null || !scoreItemRepo.existsById(e.getScoreItemId()));
        e = saveToDB(e, token, isCreate);

        ScoreItem newScoreItem = scoreItemRepo.findById(e.getScoreItemId()).orElseThrow();

        if (isCreate) {
            kafkaScoreItemProducer.sendMessage(e, KAFKA_TOPIC_CREATE);
        } else {
            kafkaScoreItemProducer.sendMessage(e, KAFKA_TOPIC_UPDATE);
        }

        redisScoreItemService.saveToRedis(e, token, NAME_SERVICE);

        return newScoreItem;
    }

    @Transactional
    private ScoreItem saveToDB(ScoreItem e, String token, boolean isCreate) {
        LocalDateTime now = CommonFunction.getCurrentTime();
        String currentUserId = redisScoreItemService.getCurrentUserId(token);

        e.setUpdateAt(now);
        e.setUpdateBy(currentUserId);
        if (isCreate) {
            e.setCreatedAt(now);
            e.setCreatedBy(currentUserId);
            e.setScoreItemId(UUID.randomUUID().toString());
        }

        e.setScoreItemId(scoreItemRepo.save(e).getScoreItemId());
        return e;
    }

    @Override
    public ScoreItem delete(String id, String token) {
        ScoreItem scoreItem = scoreItemRepo.findById(id).orElseThrow();
        String userId = redisScoreItemService.getCurrentUserId(token);

        scoreItemRepo.deleteById(id);

        // Rebuild cache theo classId
        List<ScoreItem> lists = scoreItemRepo.findAllByUserId(userId);
        redisScoreItemService.saveListToRedis(lists, token, NAME_SERVICE);

        return scoreItem;
    }

}
