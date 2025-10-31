package com.stacklog.score_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacklog.core_service.utils.jwt.JwtDecoder;
import com.stacklog.core_service.utils.redis.RedisService;
import com.stacklog.score_service.model.entities.ScoreCategory;
import com.stacklog.score_service.model.entities.ScoreItem;

@Configuration
public class RedisScoreConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("redis", 6379);
    }

    @Bean
    public RedisService<ScoreCategory> redisScoreCategoryService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            JwtDecoder jwtDecoder) {
        return new RedisService<>(ScoreCategory.class, jwtDecoder, ScoreCategory::getScoreCategoryId);
    }

    @Bean
    public RedisService<ScoreItem> redisScoreItemService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            JwtDecoder jwtDecoder) {
        return new RedisService<>(ScoreItem.class, jwtDecoder, ScoreItem::getScoreItemId);
    }

}
