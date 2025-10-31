package com.stacklog.score_service.model.entities;

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
public class ScoreItem extends CoreEntity {
    
    @Id
    private String scoreItemId;

    private String scoreItemName;
    private Double scoreItemValue = 0.00;
    private Boolean isVisualize = false;

    private String userId;

    private String groupId;

    @ManyToOne
    @JoinColumn(name = "scoreCategoryId")
    @JsonBackReference("category-item")
    private ScoreCategory scoreCategory;


}
