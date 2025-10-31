package com.stacklog.document_service.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.stacklog.core_service.model.entities.CoreEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class DocumentLocation extends CoreEntity {
    
    @Id
    private String documentLocationId;

    private String groupId;

    @ManyToOne
    @JoinColumn(name = "documentId")
    @JsonBackReference("document-location")
    private Document document;

}
