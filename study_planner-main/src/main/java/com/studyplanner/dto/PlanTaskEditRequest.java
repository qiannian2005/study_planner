package com.studyplanner.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlanTaskEditRequest {
    private Long id;
    private Integer dayNumber;
    private String content;
    private BigDecimal duration;
    private LocalDate scheduledDate;
    private Integer sortOrder;
    private Integer isCompleted;
}
