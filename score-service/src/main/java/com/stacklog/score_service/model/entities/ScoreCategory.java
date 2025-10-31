package com.stacklog.score_service.model.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.stacklog.core_service.model.entities.CoreEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
public class ScoreCategory extends CoreEntity {
    
    @Id
    private String scoreCategoryId;

    private String scoreCategoryName;
    private Double scoreCategoryWeight;
    private Double scoreCategoryComment;
    private String classId;

    private Boolean isReusable = true;

    @OneToMany(mappedBy = "scoreCategory", cascade = CascadeType.REMOVE)
    @JsonManagedReference("category-item")
    private List<ScoreItem> scoreItems;

}
