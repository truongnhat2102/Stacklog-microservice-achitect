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
import com.stacklog.score_service.model.entities.ScoreCategory;
import com.stacklog.score_service.model.repo.ScoreCategoryRepo;

import jakarta.transaction.Transactional;

@Service
public class ScoreCategoryService implements IService<ScoreCategory> {

    private static final String NAME_SERVICE = "score-service";

    private static final String KAFKA_TOPIC_UPDATE = "score-service.scorecategory.updated";
    private static final String KAFKA_TOPIC_CREATE = "score-service.scorecategory.created";

    @Autowired
    private ScoreCategoryRepo scoreCategoryRepo;

    @Autowired
    private KafkaProducer<ScoreCategory> kafkaScoreCategoryProducer;

    private final RedisService<ScoreCategory> redisScoreCategoryService;

    public ScoreCategoryService(RedisService<ScoreCategory> redisScoreCategoryService) {
        this.redisScoreCategoryService = redisScoreCategoryService;
    }

    @Override
    public List<ScoreCategory> getAllByUserId(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllByUserId'");
    }

    public List<ScoreCategory> getAllByClassId(String classId, String token) {
        String suffix = "class:" + classId;
        List<ScoreCategory> lists = redisScoreCategoryService.getAllBySuffix(token, NAME_SERVICE, suffix);
        if (lists == null || lists.isEmpty()) {
            lists = scoreCategoryRepo.findAllByClassId(classId);
            redisScoreCategoryService.saveListToRedisWithSuffix(lists, token, NAME_SERVICE, suffix);
        }
        return lists;
    }

    public List<ScoreCategory> getAllByReused(String token) {
        List<ScoreCategory> list = redisScoreCategoryService.getAll(token, NAME_SERVICE);
        if (list == null || list.isEmpty()) {
            list = scoreCategoryRepo.findAllByIsReusable(true);
            redisScoreCategoryService.saveListToRedis(list, token, NAME_SERVICE);
        } else {
            list = list.stream().filter(sc -> sc.getIsReusable() == true).toList();
        }
        return list;
    }

    @Override
    public ScoreCategory getById(String id, String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public ScoreCategory save(ScoreCategory e, String token) {
        boolean isCreate = (e.getScoreCategoryId() == null || !scoreCategoryRepo.existsById(e.getScoreCategoryId()));
        e = saveToDB(e, token, isCreate);

        ScoreCategory newScoreCategory = scoreCategoryRepo.findById(e.getScoreCategoryId()).orElseThrow();

        if (isCreate) {
            kafkaScoreCategoryProducer.sendMessage(e, KAFKA_TOPIC_CREATE);
        } else {
            kafkaScoreCategoryProducer.sendMessage(e, KAFKA_TOPIC_UPDATE);
        }

        redisScoreCategoryService.saveToRedis(e, token, NAME_SERVICE);

        return newScoreCategory;
    }

    @Transactional
    private ScoreCategory saveToDB(ScoreCategory e, String token, boolean isCreate) {
        LocalDateTime now = CommonFunction.getCurrentTime();
        String currentUserId = redisScoreCategoryService.getCurrentUserId(token);

        e.setUpdateAt(now);
        e.setUpdateBy(currentUserId);
        if (isCreate) {
            e.setCreatedAt(now);
            e.setCreatedBy(currentUserId);
            e.setScoreCategoryId(UUID.randomUUID().toString());
        }
        e = scoreCategoryRepo.save(e);
        
        return e;
    }

    @Override
    public ScoreCategory delete(String id, String token) {
        ScoreCategory scoreCategory = scoreCategoryRepo.findById(id).orElseThrow();

        scoreCategoryRepo.deleteById(id);

        // Rebuild cache theo classId
        List<ScoreCategory> lists = scoreCategoryRepo.findAllByClassId(scoreCategory.getClassId());
        redisScoreCategoryService.saveListToRedis(lists, token, NAME_SERVICE);

        return scoreCategory;
    }
    
}
