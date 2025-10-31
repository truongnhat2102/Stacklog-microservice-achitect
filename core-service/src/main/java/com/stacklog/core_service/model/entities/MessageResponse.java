package com.stacklog.core_service.model.entities;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MessageResponse {

    private String messageResponseId;

    private String messageResponseContent;
    private LocalDateTime messageResponseTime;


}
