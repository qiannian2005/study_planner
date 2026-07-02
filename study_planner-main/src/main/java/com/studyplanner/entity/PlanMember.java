package com.studyplanner.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanMember {
    private Long id;
    private Long planId;
    private Long userId;
    private String role;
    private String status;
    private LocalDateTime joinedAt;

    private String username;
    private String avatar;
}
