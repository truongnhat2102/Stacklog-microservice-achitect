package com.stacklog.core_service.model.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public class CoreEntity {

    static final Logger logger = LoggerFactory.getLogger(CoreEntity.class);

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_by")
    private String updateBy;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    public CoreEntity(String createdBy, String createdAt, String updateBy, String updateAt) {
        this.createdBy = createdBy;
        this.updateBy = updateBy;
        try {
            this.createdAt = convertTime(createdAt);
            this.updateAt = convertTime(updateAt);
        } catch (Exception e) {
            logger.error("Failed to parse date", e);
            this.createdAt = null;
            this.updateAt = null;
        }
    }

    public CoreEntity() {
    }

    public LocalDateTime convertTime(String dateString) throws Exception {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
            return dateTime;
        } catch (Exception e) {
            throw new Exception("You need to have dateString like yyyy-MM-dd HH:mm:ss");
        }
    }

}
