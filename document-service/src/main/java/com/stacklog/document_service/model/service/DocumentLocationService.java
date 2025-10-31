package com.stacklog.document_service.model.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stacklog.core_service.utils.CommonFunction;
import com.stacklog.core_service.utils.redis.RedisService;
import com.stacklog.document_service.model.entities.DocumentLocation;
import com.stacklog.document_service.model.repo.DocumentLocationRepo;

import jakarta.transaction.Transactional;

@Service
public class DocumentLocationService {

    private final RedisService<DocumentLocation> redisDocumentLocationService;

    public DocumentLocationService (RedisService<DocumentLocation> redisService) {
        this.redisDocumentLocationService = redisService;
    }

    @Autowired
    private DocumentLocationRepo documentLocationRepo;
    
    public void deleteDocumentLocation(String documentId, List<DocumentLocation> assignsFE) {
        Set<String> keepIds = assignsFE.stream()
                .map(DocumentLocation::getDocumentLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (keepIds == null || keepIds.isEmpty()) {
            documentLocationRepo.deleteAllByDocumentId(documentId);
            return;
        } 
        documentLocationRepo.deleteAllNotIn(documentId, keepIds);
    }

    @Transactional
    public DocumentLocation save(DocumentLocation e, String token) {
        DocumentLocation documentLocation = documentLocationRepo.findByDocumentDocumentIdAndGroupId(e.getDocument().getDocumentId(), e.getGroupId());
        boolean isCreate = (documentLocation == null);
        e.setUpdateAt(CommonFunction.getCurrentTime());
        e.setUpdateBy(redisDocumentLocationService.getCurrentUserId(token));
        if (isCreate) {
            e.setCreatedAt(CommonFunction.getCurrentTime());
            e.setCreatedBy(redisDocumentLocationService.getCurrentUserId(token));
            e.setDocumentLocationId(UUID.randomUUID().toString());
        } else {
            e.setDocumentLocationId(documentLocation.getDocumentLocationId());
        }

        e = documentLocationRepo.save(e);

        return e;
    }
    
}
