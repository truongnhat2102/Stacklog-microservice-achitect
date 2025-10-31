package com.stacklog.document_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacklog.core_service.utils.jwt.JwtDecoder;
import com.stacklog.core_service.utils.redis.RedisService;
import com.stacklog.document_service.model.entities.Document;
import com.stacklog.document_service.model.entities.DocumentLocation;

@Configuration
public class RedisDocumentConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("redis", 6379);
    }

    @Bean
    public RedisService<Document> redisDocumentService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            JwtDecoder jwtDecoder) {
        return new RedisService<>(Document.class, jwtDecoder, Document::getDocumentId);
    }

    @Bean
    public RedisService<DocumentLocation> redisDocumentLocationServie(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            JwtDecoder jwtDecoder) {
        return new RedisService<>(DocumentLocation.class, jwtDecoder, DocumentLocation::getDocumentLocationId);
    }

}
