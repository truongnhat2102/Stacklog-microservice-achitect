package com.stacklog.document_service.model.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stacklog.core_service.model.service.IService;
import com.stacklog.core_service.utils.CommonFunction;
import com.stacklog.core_service.utils.kafka.KafkaProducer;
import com.stacklog.core_service.utils.redis.RedisService;
import com.stacklog.document_service.model.entities.Document;
import com.stacklog.document_service.model.repo.DocumentRepo;
import jakarta.transaction.Transactional;

@Service
public class DocumentService implements IService<Document> {

    private static final String NAME_SERVICE = "document-service";

    private static final String KAFKA_TOPIC_UPDATE = "document-service.document.updated";
    private static final String KAFKA_TOPIC_CREATE = "document-service.document.created";

    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private DocumentLocationService documentLocationService;

    @Autowired
    private ClassServiceClient classServiceClient;

    @Autowired
    private KafkaProducer<Document> kafkaDocumentProducer;

    private final RedisService<Document> redisDocumentService;

    public DocumentService(RedisService<Document> redisService, DocumentRepo documentRepo) {
        this.redisDocumentService = redisService;
        this.documentRepo = documentRepo;
    }

    @Override
    public Document delete(String id, String token) {
        Document document = documentRepo.findById(id).orElseThrow();
        String userId = redisDocumentService.getCurrentUserId(token);

        documentRepo.deleteById(id);

        // Rebuild cache theo user
        List<Document> byUser = documentRepo.findByUserId(userId);
        redisDocumentService.saveListToRedis(byUser, token, NAME_SERVICE);

        return document;
    }

    @Override
    public List<Document> getAllByUserId(String token) {
        List<Document> list = redisDocumentService.getAll(token, NAME_SERVICE);
        if (list == null || list.isEmpty()) {
            list = documentRepo.findByUserId(redisDocumentService.getCurrentUserId(token));
            List<String> groupIds = classServiceClient.getGroupByUserId(token)
                    .stream()
                    .map(Groupss::getGroupsId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();

            if (groupIds.isEmpty())
                return List.of();
            
            list.addAll(documentRepo.findUserDocumentByGroupIds(groupIds));
            redisDocumentService.saveListToRedis(list, token, NAME_SERVICE);
        }
        list.sort((d1,d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()));
        return list;
    }

    @Override
    public Document getById(String id, String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    public List<Document> getByGroupId(String groupId, String token) {
        return documentRepo.findAllByDocumentLocationGroupId(groupId);
    }

    @Override
    @Transactional
    public Document save(Document e, String token) {
        boolean isCreate = (e.getDocumentId() == null || !documentRepo.existsById(e.getDocumentId()));
        e = saveToDB(e, token, isCreate);

        if (e.getDocumentLocations() != null && !e.getDocumentLocations().isEmpty()) {
            if (!isCreate) {
                documentLocationService.deleteDocumentLocation(e.getDocumentId(), e.getDocumentLocations());
            }
            e.getDocumentLocations().stream().forEach(a -> documentLocationService.save(a, token));
        }

        Document newDocument = documentRepo.findByDocumentId(e.getDocumentId());

        if (isCreate) {
            kafkaDocumentProducer.sendMessage(e, KAFKA_TOPIC_CREATE);
        } else {
            kafkaDocumentProducer.sendMessage(e, KAFKA_TOPIC_UPDATE);
        }

        redisDocumentService.saveToRedis(e, token, NAME_SERVICE);

        newDocument.setDocumentLocations(e.getDocumentLocations());

        return newDocument;
    }

    @Transactional
    private Document saveToDB(Document e, String token, boolean isCreate) {
        LocalDateTime now = CommonFunction.getCurrentTime();
        String currentUserId = redisDocumentService.getCurrentUserId(token);

        e.setUpdateAt(now);
        e.setUpdateBy(currentUserId);
        if (isCreate) {
            e.setCreatedAt(now);
            e.setCreatedBy(currentUserId);
            e.setDocumentId(UUID.randomUUID().toString());
        }

        e.setDocumentId(documentRepo.save(e).getDocumentId());
        return e;
    }

}
