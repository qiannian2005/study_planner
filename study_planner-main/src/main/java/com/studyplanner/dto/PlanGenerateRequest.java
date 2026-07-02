package com.studyplanner.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PlanGenerateRequest {

    @NotBlank(message = "学习目标不能为空")
    @Size(max = 500, message = "学习目标描述不能超过500字")
    private String goal;

    @NotBlank(message = "基础水平不能为空")
    private String level;

    @NotNull(message = "每日学习时长不能为空")
    @DecimalMin(value = "0.5", message = "每日学习时长至少0.5小时")
    @DecimalMax(value = "4", message = "每日学习时长不能超过4小时")
    private BigDecimal dailyHours;

    @NotNull(message = "计划天数不能为空")
    @Min(value = 1, message = "计划天数至少1天")
    @Max(value = 30, message = "计划天数不能超过30天")
    private Integer totalDays;

    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    private String notes;
    private String studyMode;
    private String studyPreference;
    private String expectedEndDate;
    private List<String> timeSlots;
    private List<String> roadmap;
    private Map<String, Object> goalAnalysis;

    private String customApiUrl;
    private String customApiKey;
    private String modelName;
}
