package com.studyplanner.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanEditVersion {
    private Long id;
    private Long planId;
    private Long userId;
    private String title;
    private String snapshot;
    private LocalDateTime createTime;
}
