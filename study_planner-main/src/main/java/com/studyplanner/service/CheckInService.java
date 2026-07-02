package com.studyplanner.service;

import com.studyplanner.entity.CheckIn;
import com.studyplanner.entity.PlanDetail;
import com.studyplanner.mapper.CheckInMapper;
import com.studyplanner.mapper.PlanDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckInService {

    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private PlanDetailMapper planDetailMapper;

    @Autowired
    private PlanService planService;

    @Autowired
    private PlanGroupService planGroupService;

    @Transactional
    public CheckIn checkIn(CheckIn checkIn) {
        if (!planGroupService.canAccessPlan(checkIn.getPlanId(), checkIn.getUserId())) {
            throw new RuntimeException("No permission to check in this plan");
        }

        PlanDetail detail = planDetailMapper.findById(checkIn.getDetailId());
        if (detail == null || !checkIn.getPlanId().equals(detail.getPlanId())) {
            throw new IllegalArgumentException("Invalid task for this plan");
        }

        if (checkIn.getCheckDate() == null) {
            checkIn.setCheckDate(LocalDate.now());
        }

        CheckIn existing = checkInMapper.findByUserIdAndDateAndDetailId(
                checkIn.getUserId(), checkIn.getCheckDate(), checkIn.getDetailId());
        if (existing != null) {
            return existing;
        }

        try {
            checkInMapper.insert(checkIn);
        } catch (DuplicateKeyException e) {
            CheckIn duplicate = checkInMapper.findByUserIdAndDateAndDetailId(
                    checkIn.getUserId(), checkIn.getCheckDate(), checkIn.getDetailId());
            if (duplicate != null) {
                return duplicate;
            }
            throw e;
        }

        if (!planGroupService.isGroupPlan(checkIn.getPlanId())) {
            planDetailMapper.updateCompleted(checkIn.getDetailId(), 1);
            checkAndUpdatePlanStatus(checkIn.getPlanId());
        }

        return checkIn;
    }

    private void checkAndUpdatePlanStatus(Long planId) {
        try {
            double progress = planService.getPlanProgress(planId);
            if (progress >= 100.0) {
                planService.updatePlanStatus(planId, "已完成");
            }
        } catch (Exception e) {
            System.err.println("Failed to update plan status: " + planId + ", " + e.getMessage());
        }
    }

    public List<CheckIn> getTodayCheckIns(Long userId) {
        return checkInMapper.findByUserIdAndDate(userId, LocalDate.now());
    }

    public List<CheckIn> getUserCheckIns(Long userId) {
        return checkInMapper.findByUserId(userId);
    }

    public Map<String, List<CheckIn>> getMonthlyCheckIns(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<CheckIn> checkIns = checkInMapper.findByUserIdAndDateRange(userId, startDate, endDate);

        Map<String, List<CheckIn>> result = new HashMap<>();
        result.put("checkIns", checkIns);

        return result;
    }

    public int getStreakDays(Long userId) {
        List<LocalDate> dates = checkInMapper.findCheckInDates(userId);

        if (dates.isEmpty()) {
            return 0;
        }

        int streak = 0;
        LocalDate today = LocalDate.now();
        LocalDate expectedDate = today;

        for (LocalDate date : dates) {
            if (date.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (date.isBefore(expectedDate)) {
                if (streak == 0 && date.equals(today.minusDays(1))) {
                    streak = 1;
                    expectedDate = date.minusDays(1);
                } else {
                    break;
                }
            }
        }

        return streak;
    }

    public Map<String, Object> getStudyStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", checkInMapper.countCheckInDays(userId));
        stats.put("totalHours", checkInMapper.sumStudyHours(userId));
        stats.put("streakDays", getStreakDays(userId));
        return stats;
    }

    public Map<String, Object> getChartData(Long userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        Map<String, Object> weekData = new HashMap<>();
        List<String> weekXAxis = new ArrayList<>();
        List<Double> weekSeries = new ArrayList<>();

        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            weekXAxis.add(getDayOfWeekCN(date.getDayOfWeek().getValue()));
            Double hours = checkInMapper.sumStudyHoursByDate(userId, date);
            weekSeries.add(hours != null ? hours : 0.0);
        }
        weekData.put("xAxis", weekXAxis);
        weekData.put("series", weekSeries);
        result.put("week", weekData);

        Map<String, Object> monthData = new HashMap<>();
        List<String> monthXAxis = new ArrayList<>();
        List<Double> monthSeries = new ArrayList<>();

        LocalDate firstDay = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();

        for (int i = 0; i < daysInMonth; i++) {
            LocalDate date = firstDay.plusDays(i);
            monthXAxis.add(date.getDayOfMonth() + "日");
            Double hours = checkInMapper.sumStudyHoursByDate(userId, date);
            monthSeries.add(hours != null ? hours : 0.0);
        }
        monthData.put("xAxis", monthXAxis);
        monthData.put("series", monthSeries);
        result.put("month", monthData);

        return result;
    }

    private String getDayOfWeekCN(int value) {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[value - 1];
    }
}
