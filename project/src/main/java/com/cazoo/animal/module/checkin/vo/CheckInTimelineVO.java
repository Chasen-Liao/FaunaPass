package com.cazoo.animal.module.checkin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInTimelineVO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;
    private String userNickname;
    private Long animalId;
    private String animalName;
}
