package com.stacklog.document_service.model.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stacklog.document_service.model.entities.Document;

@Repository
public interface DocumentRepo extends JpaRepository<Document, String> {

    Document findByDocumentId(String documentId);

    @Query("SELECT DISTINCT d FROM Document d JOIN d.documentLocations dl WHERE dl.groupId = :groupId")
    List<Document> findAllByDocumentLocationGroupId(String groupId);

    @Query("SELECT d FROM Document d WHERE d.createdBy = :userId OR d.updateBy = :userId")
    List<Document> findByUserId(@Param("userId") String userId);

    @Query("""
            select distinct d
            from Document d
            join fetch d.documentLocations dl
            where dl.groupId in :groupIds
                """)
    List<Document> findUserDocumentByGroupIds(@Param("groupIds") List<String> groupIds);

}
