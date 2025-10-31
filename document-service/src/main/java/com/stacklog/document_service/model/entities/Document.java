package com.stacklog.document_service.model.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.stacklog.core_service.model.entities.CoreEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Document extends CoreEntity {
    
    @Id
    private String documentId;

    private String documentTitle;
    private String documentContentType;
    private Integer documentSize;

    @Enumerated(EnumType.STRING) // hoặc EnumType.ORDINAL
    private DocumentType documentType;

    public enum DocumentType {
        NORMAL, REPORT
    }

    private String documentPath;

    @OneToMany(mappedBy = "document", cascade = CascadeType.REMOVE)
    @JsonManagedReference("document-location")
    private List<DocumentLocation> documentLocations;


}
