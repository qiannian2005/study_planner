package com.studyplanner.controller;

import com.studyplanner.dto.ApiResponse;
import com.studyplanner.service.ForumService;
import com.studyplanner.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ForumService forumService;

    public AdminController(UserService userService, ForumService forumService) {
        this.userService = userService;
        this.forumService = forumService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getUsers(
            @RequestParam(required = false) String keyword,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success(userService.getAdminUsers(keyword));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        userService.updateUserStatus(id, String.valueOf(request.getOrDefault("status", "")));
        return ApiResponse.success("用户状态已更新", null);
    }

    @PutMapping("/users/{id}/password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        userService.resetUserPassword(id, String.valueOf(request.getOrDefault("password", "")));
        return ApiResponse.success("Password reset successfully", null);
    }

    @GetMapping("/topics")
    public ApiResponse<List<Map<String, Object>>> getTopics(HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success(forumService.getAdminTopics());
    }

    @PostMapping("/topics")
    public ApiResponse<Map<String, Object>> createTopic(
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success("话题已创建", forumService.createAdminTopic(request));
    }

    @PutMapping("/topics/{id}")
    public ApiResponse<Map<String, Object>> updateTopic(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success("话题已更新", forumService.updateAdminTopic(id, request));
    }

    @PutMapping("/topics/{id}/status")
    public ApiResponse<Void> updateTopicStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        forumService.updateTopicStatus(id, String.valueOf(request.getOrDefault("status", "")));
        return ApiResponse.success("话题状态已更新", null);
    }

    @DeleteMapping("/topics/{id}")
    public ApiResponse<Void> deleteTopic(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        forumService.deleteTopic(id);
        return ApiResponse.success("Topic deleted", null);
    }

    @GetMapping("/topic-suggestions")
    public ApiResponse<List<Map<String, Object>>> getTopicSuggestions(HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        return ApiResponse.success(forumService.getAdminTopicSuggestions());
    }

    @PutMapping("/topic-suggestions/{id}/approve")
    public ApiResponse<Map<String, Object>> approveTopicSuggestion(@PathVariable Long id, HttpSession session) {
        Long reviewerId = currentUserId(session);
        if (reviewerId == null || !isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        return ApiResponse.success("Topic suggestion approved", forumService.approveTopicSuggestion(id, reviewerId));
    }

    @PutMapping("/topic-suggestions/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectTopicSuggestion(@PathVariable Long id, HttpSession session) {
        Long reviewerId = currentUserId(session);
        if (reviewerId == null || !isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        return ApiResponse.success("Topic suggestion rejected", forumService.rejectTopicSuggestion(id, reviewerId));
    }

    @DeleteMapping("/topic-suggestions/{id}")
    public ApiResponse<Void> deleteTopicSuggestion(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("No admin permission");
        }
        forumService.deleteTopicSuggestion(id);
        return ApiResponse.success("Topic suggestion deleted", null);
    }

    @GetMapping("/questions")
    public ApiResponse<List<Map<String, Object>>> getQuestions(HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success(forumService.getAdminQuestions());
    }

    @PutMapping("/questions/{id}/status")
    public ApiResponse<Void> updateQuestionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        forumService.updateQuestionStatus(id, String.valueOf(request.getOrDefault("status", "")));
        return ApiResponse.success("问题状态已更新", null);
    }

    @GetMapping("/answers")
    public ApiResponse<List<Map<String, Object>>> getAnswers(HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success(forumService.getAdminAnswers());
    }

    @PutMapping("/answers/{id}/status")
    public ApiResponse<Void> updateAnswerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        forumService.updateAnswerStatus(id, String.valueOf(request.getOrDefault("status", "")));
        return ApiResponse.success("回答状态已更新", null);
    }

    @GetMapping("/comments")
    public ApiResponse<List<Map<String, Object>>> getComments(HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        return ApiResponse.success(forumService.getAdminComments());
    }

    @PutMapping("/comments/{id}/status")
    public ApiResponse<Void> updateCommentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        forumService.updateCommentStatus(id, String.valueOf(request.getOrDefault("status", "")));
        return ApiResponse.success("评论状态已更新", null);
    }

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return userId != null && userService.isAdmin(userId);
    }

    private Long currentUserId(HttpSession session) {
        Object value = session.getAttribute("userId");
        return value instanceof Long id ? id : null;
    }
}
