package com.stacklog.core_service.utils.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KafkaProducer<E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);

    private KafkaTemplate<String, E> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, E> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Autowired
    ObjectMapper objectMapper;

    public void sendMessage(E e, String topic) {
        try {
            LOGGER.info("Sending Kafka message: {} => {}", e.getClass().getSimpleName(), objectMapper.writeValueAsString(e));
        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        }

        // create message
        Message<E> message = MessageBuilder
                .withPayload(e)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();
        kafkaTemplate.send(message);
    }

}
