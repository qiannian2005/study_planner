package com.studyplanner.controller;

import com.studyplanner.dto.ApiResponse;
import com.studyplanner.dto.PageResult;
import com.studyplanner.service.ForumService;
import com.studyplanner.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService forumService;
    private final UserService userService;

    public ForumController(ForumService forumService, UserService userService) {
        this.forumService = forumService;
        this.userService = userService;
    }

    @PostConstruct
    public void initializeForumTables() {
        forumService.initializeSchema();
    }

    @GetMapping("/question")
    public ApiResponse<PageResult<Map<String, Object>>> getQuestions(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        Long userId = currentUserId(session);
        if (Boolean.TRUE.equals(favorite)) {
            if (userId == null) {
                return ApiResponse.unauthorized("璇峰厛鐧诲綍");
            }
            return ApiResponse.success(forumService.getFavoriteQuestionsPage(userId, sort, page, size));
        }
        return ApiResponse.success(forumService.getQuestionsPage(sort, userId, page, size));
    }

    @GetMapping("/question/{id}")
    public ApiResponse<Map<String, Object>> getQuestion(@PathVariable Long id, HttpSession session) {
        Map<String, Object> question = forumService.getQuestion(id, currentUserId(session), session.getId());
        if (question == null) {
            return ApiResponse.error(404, "问题不存在");
        }
        return ApiResponse.success(question);
    }

    @PostMapping("/question")
    public ApiResponse<Map<String, Object>> createQuestion(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String title = String.valueOf(request.getOrDefault("title", "")).trim();
        if (title.isEmpty()) {
            return ApiResponse.badRequest("标题不能为空");
        }

        return ApiResponse.success("发布成功", forumService.createQuestion(userId, request));
    }

    @GetMapping("/question/{id}/answers")
    public ApiResponse<List<Map<String, Object>>> getAnswers(
            @PathVariable Long id,
            @RequestParam(required = false) String sort,
            HttpSession session
    ) {
        if (forumService.getQuestion(id) == null) {
            return ApiResponse.error(404, "问题不存在");
        }
        return ApiResponse.success(forumService.getAnswers(id, sort, currentUserId(session)));
    }

    @PostMapping("/answer")
    public ApiResponse<Map<String, Object>> createAnswer(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String content = String.valueOf(request.getOrDefault("content", "")).trim();
        if (content.isEmpty()) {
            return ApiResponse.badRequest("回答内容不能为空");
        }

        return ApiResponse.success("回答成功", forumService.createAnswer(userId, request));
    }

    @PostMapping("/answer/{id}/vote")
    public ApiResponse<Map<String, Object>> voteAnswer(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.voteAnswer(userId, id));
    }

    @PostMapping("/answer/{id}/collect")
    public ApiResponse<Map<String, Object>> collectAnswer(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.collectAnswer(userId, id));
    }

    @GetMapping("/comment")
    public ApiResponse<List<Map<String, Object>>> getComments(@RequestParam("answer_id") Long answerId) {
        return ApiResponse.success(forumService.getComments(answerId));
    }

    @PostMapping("/comment")
    public ApiResponse<Map<String, Object>> createComment(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }

        String content = String.valueOf(request.getOrDefault("content", "")).trim();
        if (content.isEmpty()) {
            return ApiResponse.badRequest("评论内容不能为空");
        }

        return ApiResponse.success("评论成功", forumService.createComment(userId, request));
    }

    @PostMapping("/comment/{id}/vote")
    public ApiResponse<Map<String, Object>> voteComment(@PathVariable Long id) {
        return ApiResponse.success(forumService.voteComment(id));
    }

    @GetMapping("/topic/hot")
    public ApiResponse<List<Map<String, Object>>> getHotTopics(HttpSession session) {
        return ApiResponse.success(forumService.getTopics(currentUserId(session)));
    }

    @GetMapping("/topic")
    public ApiResponse<List<Map<String, Object>>> getTopics(HttpSession session) {
        return ApiResponse.success(forumService.getTopics(currentUserId(session)));
    }

    @GetMapping("/topic/{id}")
    public ApiResponse<Map<String, Object>> getTopic(@PathVariable Long id, HttpSession session) {
        Map<String, Object> topic = forumService.getTopic(id, currentUserId(session));
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        return ApiResponse.success(topic);
    }

    @GetMapping("/topic/{id}/questions")
    public ApiResponse<PageResult<Map<String, Object>>> getTopicQuestions(
            @PathVariable Long id,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(forumService.getTopicQuestionsPage(id, sort, page, size));
    }

    @GetMapping("/topic/{id}/top-answerers")
    public ApiResponse<List<Map<String, Object>>> getTopAnswerersByTopic(@PathVariable Long id) {
        return ApiResponse.success(forumService.getTopAnswerersByTopic(id));
    }

    @GetMapping("/topic/{id}/top-answers")
    public ApiResponse<List<Map<String, Object>>> getTopAnswersByTopic(@PathVariable Long id) {
        return ApiResponse.success(forumService.getTopAnswersByTopic(id));
    }

    @PostMapping("/topic")
    public ApiResponse<Map<String, Object>> createTopic(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!isAdmin(session)) {
            return ApiResponse.forbidden("无管理员权限");
        }
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        if (name.isEmpty()) {
            return ApiResponse.badRequest("话题名称不能为空");
        }
        return ApiResponse.success(forumService.createTopic(request));
    }

    @PostMapping("/topic/suggestion")
    public ApiResponse<Map<String, Object>> suggestTopic(@RequestBody Map<String, Object> request, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("Please log in first");
        }
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        if (name.isEmpty()) {
            return ApiResponse.badRequest("Topic name is required");
        }
        return ApiResponse.success("Topic suggestion submitted", forumService.createTopicSuggestion(userId, request));
    }

    @PostMapping("/question/{id}/favorite")
    public ApiResponse<Map<String, Object>> favoriteQuestion(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.favoriteQuestion(userId, id));
    }

    @PostMapping("/question/{id}/vote")
    public ApiResponse<Map<String, Object>> voteQuestion(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.voteQuestion(userId, id));
    }

    @PostMapping("/question/{id}/follow")
    public ApiResponse<Map<String, Object>> followQuestion(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("璇峰厛鐧诲綍");
        }
        return ApiResponse.success(forumService.followQuestion(userId, id));
    }

    @PostMapping("/topic/{id}/follow")
    public ApiResponse<Map<String, Object>> followTopic(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.followTopic(userId, id));
    }

    @GetMapping("/user/{id}")
    public ApiResponse<Map<String, Object>> getForumUser(@PathVariable Long id, HttpSession session) {
        Map<String, Object> user = forumService.getForumUser(id, currentUserId(session));
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        return ApiResponse.success(user);
    }

    @GetMapping("/user/{id}/questions")
    public ApiResponse<PageResult<Map<String, Object>>> getUserQuestions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        return ApiResponse.success(forumService.getUserQuestionsPage(id, currentUserId(session), page, size));
    }

    @GetMapping("/user/{id}/answers")
    public ApiResponse<List<Map<String, Object>>> getUserAnswers(@PathVariable Long id, HttpSession session) {
        return ApiResponse.success(forumService.getUserAnswers(id, currentUserId(session)));
    }

    @GetMapping("/user/{id}/collections")
    public ApiResponse<List<Map<String, Object>>> getUserCollections(@PathVariable Long id) {
        return ApiResponse.success(forumService.getMyCollections(id));
    }

    @PostMapping("/user/{id}/follow")
    public ApiResponse<Map<String, Object>> followUser(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.followUser(userId, id));
    }

    @GetMapping("/user/{id}/following")
    public ApiResponse<List<Map<String, Object>>> getUserFollowing(@PathVariable Long id) {
        return ApiResponse.success(forumService.getFollowingUsers(id));
    }

    @GetMapping("/user/{id}/followers")
    public ApiResponse<List<Map<String, Object>>> getUserFollowers(@PathVariable Long id) {
        return ApiResponse.success(forumService.getFollowerUsers(id));
    }

    @GetMapping("/my/questions")
    public ApiResponse<PageResult<Map<String, Object>>> getMyQuestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.getMyQuestionsPage(userId, page, size));
    }

    @GetMapping("/my/answers")
    public ApiResponse<List<Map<String, Object>>> getMyAnswers(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.getMyAnswers(userId));
    }

    @GetMapping("/my/collections")
    public ApiResponse<List<Map<String, Object>>> getMyCollections(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.getMyCollections(userId));
    }

    @GetMapping("/my/following")
    public ApiResponse<Map<String, Object>> getMyFollowing(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("questions", forumService.getFollowedQuestionsPage(userId, page, size));
        result.put("topics", forumService.getFollowedTopics(userId));
        result.put("users", forumService.getFollowingUsers(userId));
        return ApiResponse.success(result);
    }

    @GetMapping("/my/followers")
    public ApiResponse<List<Map<String, Object>>> getMyFollowers(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.getFollowerUsers(userId));
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(forumService.search(keyword, type, sort, page, size));
    }

    @GetMapping("/user/stats")
    public ApiResponse<Map<String, Object>> getUserStats(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return ApiResponse.unauthorized("请先登录");
        }
        return ApiResponse.success(forumService.getUserStats(userId));
    }

    private Long currentUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private boolean isAdmin(HttpSession session) {
        Long userId = currentUserId(session);
        return userId != null && userService.isAdmin(userId);
    }

}
