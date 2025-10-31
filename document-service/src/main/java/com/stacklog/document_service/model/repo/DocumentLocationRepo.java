package com.stacklog.document_service.model.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stacklog.document_service.model.entities.DocumentLocation;

@Repository
public interface DocumentLocationRepo extends JpaRepository<DocumentLocation, String> {

    @Modifying
    @Query("delete from DocumentLocation dl where dl.document.documentId = :documentId")
    void deleteAllByDocumentId(String documentId);

    @Modifying
    @Query("delete from DocumentLocation dl where dl.document.documentId = :documentId and dl.documentLocationId not in :keepIds")
    void deleteAllNotIn(String documentId, Set<String> keepIds);

    DocumentLocation findByDocumentDocumentIdAndGroupId(String documentId, String groupId);
    
}
