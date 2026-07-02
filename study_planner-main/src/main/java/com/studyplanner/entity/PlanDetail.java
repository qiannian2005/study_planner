package com.studyplanner.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划详情实体类 - 每日任务
 */
@Data
public class PlanDetail {
    
    /**
     * 详情ID
     */
    private Long id;
    
    /**
     * 计划ID
     */
    private Long planId;
    
    /**
     * 第几天
     */
    private Integer dayNumber;
    
    /**
     * 学习内容
     */
    private String content;
    
    /**
     * 预计时长(小时)
     */
    private BigDecimal duration;
    
    /**
     * Scheduled learning date.
     */
    private LocalDate scheduledDate;

    /**
     * Sort order within a scheduled date.
     */
    private Integer sortOrder;
    
    /**
     * 是否完成(0-未完成/1-已完成)
     */
    private Integer isCompleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
