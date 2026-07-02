package com.studyplanner.service;

import com.studyplanner.dto.PageResult;
import com.studyplanner.mapper.ForumMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

@Service
public class ForumService {

    private final ForumMapper forumMapper;
    private final JdbcTemplate jdbcTemplate;

    public ForumService(ForumMapper forumMapper, JdbcTemplate jdbcTemplate) {
        this.forumMapper = forumMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void initializeSchema() {
        forumMapper.createTopicTable();
        forumMapper.createQuestionTable();
        forumMapper.createQuestionTopicTable();
        forumMapper.createAnswerTable();
        forumMapper.createCommentTable();
        forumMapper.createQuestionVoteTable();
        forumMapper.createQuestionFavoriteTable();
        forumMapper.createQuestionFollowTable();
        forumMapper.createQuestionViewTable();
        forumMapper.createAnswerVoteTable();
        forumMapper.createAnswerCollectionTable();
        forumMapper.createUserFollowTable();
        forumMapper.createTopicFollowTable();
        forumMapper.createTopicSuggestionTable();
        ensureStatusColumns();
        forumMapper.refreshAllQuestionFollowCounts();
        forumMapper.refreshAllQuestionViewCounts();

        if (forumMapper.countTopics() == 0) {
            createTopicInternal("学习方法", "交流学习效率、时间管理和复盘方法");
            createTopicInternal("编程入门", "Python、Java、前端等编程学习问题");
            createTopicInternal("考研备考", "考研计划、资料选择和进度管理");
            createTopicInternal("英语学习", "单词、听力、阅读和写作训练");
            createTopicInternal("AI工具", "用AI辅助学习和制定计划");
        }
    }

    public List<Map<String, Object>> getQuestions(String sort) {
        List<Map<String, Object>> questions = forumMapper.findQuestions();
        questions.forEach(this::decorateQuestion);
        if ("hot".equals(sort)) {
            sortByHotScore(questions);
        }
        return questions;
    }

    public List<Map<String, Object>> getQuestions(String sort, Long userId) {
        List<Map<String, Object>> questions = getQuestions(null);
        questions.forEach(question -> decorateQuestionInteraction(question, userId));
        if ("recommend".equals(sort)) {
            sortByRecommendScore(questions, userId);
        } else if ("hot".equals(sort)) {
            sortByHotScore(questions);
        }
        return questions;
    }

    /**
     * 分页获取问题列表。
     * hot/recommend 依赖全量打分，无法 SQL 分页，采用内存排序后截取；
     * 默认排序走 SQL 分页。
     */
    public PageResult<Map<String, Object>> getQuestionsPage(String sort, Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;

        boolean memorySort = "hot".equals(sort) || "recommend".equals(sort);
        if (memorySort) {
            List<Map<String, Object>> all = getQuestions(sort, userId);
            long total = all.size();
            List<Map<String, Object>> records = slice(all, offset, safeSize);
            return new PageResult<>(records, total, safePage, safeSize);
        }

        long total = forumMapper.countQuestions();
        List<Map<String, Object>> questions = forumMapper.findQuestionsPaged(offset, safeSize);
        questions.forEach(q -> {
            decorateQuestion(q);
            decorateQuestionInteraction(q, userId);
        });
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    /**
     * 分页获取当前用户收藏的问题。
     */
    public PageResult<Map<String, Object>> getFavoriteQuestionsPage(Long userId, String sort, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;

        boolean memorySort = "hot".equals(sort) || "recommend".equals(sort);
        if (memorySort) {
            List<Map<String, Object>> all = getFavoriteQuestions(userId, sort);
            long total = all.size();
            List<Map<String, Object>> records = slice(all, offset, safeSize);
            return new PageResult<>(records, total, safePage, safeSize);
        }

        long total = forumMapper.countFavoriteQuestionsByUser(userId);
        List<Map<String, Object>> questions = forumMapper.findFavoriteQuestionsByUserPaged(userId, offset, safeSize);
        questions.forEach(q -> {
            decorateQuestion(q);
            decorateQuestionInteraction(q, userId);
        });
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    /**
     * 列表截取工具，越界自动收敛。
     */
    private <T> List<T> slice(List<T> list, int offset, int size) {
        if (offset >= list.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(offset + size, list.size());
        return new ArrayList<>(list.subList(offset, toIndex));
    }

    public List<Map<String, Object>> getFavoriteQuestions(Long userId, String sort) {
        List<Map<String, Object>> questions = forumMapper.findFavoriteQuestionsByUser(userId);
        questions.forEach(question -> {
            decorateQuestion(question);
            decorateQuestionInteraction(question, userId);
        });
        if ("recommend".equals(sort)) {
            sortByRecommendScore(questions, userId);
        } else if ("hot".equals(sort)) {
            sortByHotScore(questions);
        }
        return questions;
    }

    public Map<String, Object> getQuestion(Long id) {
        Map<String, Object> question = forumMapper.findQuestionById(id);
        if (question != null) {
            decorateQuestion(question);
        }
        return question;
    }

    public Map<String, Object> getQuestion(Long id, Long userId) {
        Map<String, Object> question = getQuestion(id);
        if (question != null) {
            decorateQuestionInteraction(question, userId);
        }
        return question;
    }

    @Transactional
    public Map<String, Object> getQuestion(Long id, Long userId, String sessionId) {
        if (forumMapper.findQuestionById(id) == null) {
            return null;
        }
        recordQuestionView(id, userId, sessionId);
        return getQuestion(id, userId);
    }

    @Transactional
    public Map<String, Object> createQuestion(Long userId, Map<String, Object> request) {
        Map<String, Object> question = new HashMap<>();
        question.put("userId", userId);
        question.put("title", request.get("title"));
        question.put("content", request.getOrDefault("content", ""));
        question.put("anonymous", Boolean.TRUE.equals(request.get("anonymous")) ? 1 : 0);
        forumMapper.insertQuestion(question);

        Long questionId = longValue(question.get("id"));
        for (Long topicId : extractTopicIds(request.get("topic_ids"))) {
            forumMapper.insertQuestionTopic(questionId, topicId);
            forumMapper.refreshTopicQuestionCount(topicId);
        }

        return getQuestion(questionId);
    }

    public List<Map<String, Object>> getTopics() {
        return forumMapper.findTopics();
    }

    public List<Map<String, Object>> getTopics(Long userId) {
        List<Map<String, Object>> topics = getTopics();
        topics.forEach(topic -> decorateTopicInteraction(topic, userId));
        return topics;
    }

    public Map<String, Object> getTopic(Long id) {
        return forumMapper.findTopicById(id);
    }

    public Map<String, Object> getTopic(Long id, Long userId) {
        Map<String, Object> topic = getTopic(id);
        if (topic != null) {
            decorateTopicInteraction(topic, userId);
        }
        return topic;
    }

    public List<Map<String, Object>> getTopicQuestions(Long topicId, String sort) {
        List<Map<String, Object>> questions = forumMapper.findQuestionsByTopic(topicId);
        questions.forEach(this::decorateQuestion);
        if ("hot".equals(sort)) {
            sortByTopicHotScore(questions);
        }
        return questions;
    }

    /**
     * 分页获取话题下的问题列表。
     */
    public PageResult<Map<String, Object>> getTopicQuestionsPage(Long topicId, String sort, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;

        if ("hot".equals(sort)) {
            List<Map<String, Object>> all = getTopicQuestions(topicId, sort);
            long total = all.size();
            return new PageResult<>(slice(all, offset, safeSize), total, safePage, safeSize);
        }

        long total = forumMapper.countQuestionsByTopic(topicId);
        List<Map<String, Object>> questions = forumMapper.findQuestionsByTopicPaged(topicId, offset, safeSize);
        questions.forEach(this::decorateQuestion);
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    private void sortByTopicHotScore(List<Map<String, Object>> questions) {
        questions.sort((left, right) -> {
            int cmp = Integer.compare(
                    intValue(right.get("answer_count")),
                    intValue(left.get("answer_count")));
            if (cmp != 0) return cmp;
            cmp = Integer.compare(
                    intValue(right.get("view_count")),
                    intValue(left.get("view_count")));
            if (cmp != 0) return cmp;
            cmp = Integer.compare(
                    intValue(right.get("follow_count")),
                    intValue(left.get("follow_count")));
            if (cmp != 0) return cmp;
            return String.valueOf(right.get("created_at"))
                    .compareTo(String.valueOf(left.get("created_at")));
        });
    }

    public List<Map<String, Object>> getTopAnswerersByTopic(Long topicId) {
        return forumMapper.findTopAnswerersByTopic(topicId);
    }

    public List<Map<String, Object>> getTopAnswersByTopic(Long topicId) {
        List<Map<String, Object>> answers = forumMapper.findTopAnswersByTopic(topicId);
        answers.forEach(answer -> {
            Object questionTitle = answer.remove("question_title");
            answer.put("question", Map.of(
                    "id", answer.get("question_id"),
                    "title", questionTitle == null ? "" : questionTitle
            ));
            decorateAuthor(answer);
        });
        return answers;
    }

    public List<Map<String, Object>> getMyQuestions(Long userId) {
        List<Map<String, Object>> questions = forumMapper.findQuestionsByUser(userId);
        questions.forEach(this::decorateQuestion);
        return questions;
    }

    /**
     * 分页获取当前用户发布的问题。
     */
    public PageResult<Map<String, Object>> getMyQuestionsPage(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;
        long total = forumMapper.countQuestionsByUser(userId);
        List<Map<String, Object>> questions = forumMapper.findQuestionsByUserPaged(userId, offset, safeSize);
        questions.forEach(this::decorateQuestion);
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    public Map<String, Object> createTopic(Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        Map<String, Object> existing = forumMapper.findTopicByName(name);
        if (existing != null) {
            return existing;
        }
        return createTopicInternal(name, String.valueOf(request.getOrDefault("description", "")));
    }

    public List<Map<String, Object>> getAdminTopics() {
        return forumMapper.findAdminTopics();
    }

    public Map<String, Object> createAdminTopic(Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        Map<String, Object> existing = forumMapper.findAnyTopicByName(name);
        if (existing != null) {
            return existing;
        }
        return createTopicInternal(name, String.valueOf(request.getOrDefault("description", "")));
    }

    public Map<String, Object> updateAdminTopic(Long id, Map<String, Object> request) {
        Map<String, Object> topic = new HashMap<>();
        topic.put("id", id);
        topic.put("name", String.valueOf(request.getOrDefault("name", "")).trim());
        topic.put("description", String.valueOf(request.getOrDefault("description", "")));
        if (String.valueOf(topic.get("name")).isBlank()) {
            throw new IllegalArgumentException("Topic name is required");
        }
        forumMapper.updateTopic(topic);
        return forumMapper.findAdminTopicById(id);
    }

    public void updateTopicStatus(Long id, String status) {
        forumMapper.updateTopicStatus(id, normalizeContentStatus(status));
    }

    public void deleteTopic(Long id) {
        forumMapper.deleteTopicById(id);
    }

    public Map<String, Object> createTopicSuggestion(Long userId, Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Topic name is required");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Topic name is too long");
        }

        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("userId", userId);
        suggestion.put("name", name);
        suggestion.put("description", limitText(request.getOrDefault("description", ""), 255));
        suggestion.put("reason", limitText(request.getOrDefault("reason", ""), 255));
        forumMapper.insertTopicSuggestion(suggestion);
        return forumMapper.findTopicSuggestionById(longValue(suggestion.get("id")));
    }

    public List<Map<String, Object>> getAdminTopicSuggestions() {
        return forumMapper.findAdminTopicSuggestions();
    }

    @Transactional
    public Map<String, Object> approveTopicSuggestion(Long id, Long reviewerId) {
        Map<String, Object> suggestion = forumMapper.findTopicSuggestionById(id);
        if (suggestion == null) {
            throw new IllegalArgumentException("Topic suggestion not found");
        }
        if (!"pending".equals(String.valueOf(suggestion.get("status")))) {
            throw new IllegalArgumentException("Topic suggestion has already been reviewed");
        }

        Map<String, Object> topic = createAdminTopic(Map.of(
                "name", suggestion.get("name"),
                "description", suggestion.getOrDefault("description", "")
        ));
        forumMapper.updateTopicSuggestionStatus(id, "approved", reviewerId);

        Map<String, Object> result = new HashMap<>();
        result.put("suggestion", forumMapper.findTopicSuggestionById(id));
        result.put("topic", topic);
        return result;
    }

    public Map<String, Object> rejectTopicSuggestion(Long id, Long reviewerId) {
        Map<String, Object> suggestion = forumMapper.findTopicSuggestionById(id);
        if (suggestion == null) {
            throw new IllegalArgumentException("Topic suggestion not found");
        }
        if (!"pending".equals(String.valueOf(suggestion.get("status")))) {
            throw new IllegalArgumentException("Topic suggestion has already been reviewed");
        }
        forumMapper.updateTopicSuggestionStatus(id, "rejected", reviewerId);
        return forumMapper.findTopicSuggestionById(id);
    }

    public void deleteTopicSuggestion(Long id) {
        forumMapper.deleteTopicSuggestionById(id);
    }

    public List<Map<String, Object>> getAdminQuestions() {
        List<Map<String, Object>> questions = forumMapper.findAdminQuestions();
        questions.forEach(this::decorateAdminContentAuthor);
        return questions;
    }

    public void updateQuestionStatus(Long id, String status) {
        forumMapper.updateQuestionStatus(id, normalizeContentStatus(status));
    }

    public List<Map<String, Object>> getAdminAnswers() {
        List<Map<String, Object>> answers = forumMapper.findAdminAnswers();
        answers.forEach(this::decorateAdminContentAuthor);
        return answers;
    }

    public void updateAnswerStatus(Long id, String status) {
        forumMapper.updateAnswerStatus(id, normalizeContentStatus(status));
    }

    public List<Map<String, Object>> getAdminComments() {
        List<Map<String, Object>> comments = forumMapper.findAdminComments();
        comments.forEach(this::decorateAdminContentAuthor);
        return comments;
    }

    public void updateCommentStatus(Long id, String status) {
        forumMapper.updateCommentStatus(id, normalizeContentStatus(status));
    }

    @Transactional
    public Map<String, Object> followTopic(Long userId, Long topicId) {
        boolean followed = forumMapper.countTopicFollow(userId, topicId) > 0;
        if (followed) {
            forumMapper.deleteTopicFollow(userId, topicId);
        } else {
            forumMapper.insertTopicFollow(userId, topicId);
        }
        forumMapper.refreshTopicFollowCount(topicId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_followed", !followed);
        result.put("follow_count", intOrZero(forumMapper.findTopicFollowCount(topicId)));
        return result;
    }

    @Transactional
    public Map<String, Object> createAnswer(Long userId, Map<String, Object> request) {
        Long questionId = longValue(request.get("question_id"));
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", questionId);
        answer.put("userId", userId);
        answer.put("content", request.get("content"));
        forumMapper.insertAnswer(answer);
        forumMapper.refreshQuestionAnswerCount(questionId);
        return answer;
    }

    public List<Map<String, Object>> getAnswers(Long questionId) {
        return getAnswers(questionId, null);
    }

    public List<Map<String, Object>> getAnswers(Long questionId, String sort) {
        List<Map<String, Object>> answers;
        if ("created_at".equals(sort) || "time".equals(sort)) {
            answers = forumMapper.findAnswersByQuestionOrderByTime(questionId);
        } else if ("vote_count".equals(sort) || "vote".equals(sort)) {
            answers = forumMapper.findAnswersByQuestionOrderByVote(questionId);
        } else {
            answers = forumMapper.findAnswersByQuestion(questionId);
        }
        answers.forEach(this::decorateAuthor);
        return answers;
    }

    public List<Map<String, Object>> getAnswers(Long questionId, String sort, Long userId) {
        List<Map<String, Object>> answers = getAnswers(questionId, sort);
        answers.forEach(answer -> decorateAnswerInteraction(answer, userId));
        return answers;
    }

    public List<Map<String, Object>> getMyAnswers(Long userId) {
        List<Map<String, Object>> answers = forumMapper.findAnswersByUser(userId);
        answers.forEach(answer -> {
            Object questionTitle = answer.remove("question_title");
            answer.put("question", Map.of(
                    "id", answer.get("question_id"),
                    "title", questionTitle == null ? "" : questionTitle
            ));
            decorateAuthor(answer);
            decorateAnswerInteraction(answer, userId);
        });
        return answers;
    }

    @Transactional
    public Map<String, Object> voteQuestion(Long userId, Long questionId) {
        boolean voted = forumMapper.countQuestionVote(userId, questionId) > 0;
        if (voted) {
            forumMapper.deleteQuestionVote(userId, questionId);
        } else {
            forumMapper.insertQuestionVote(userId, questionId);
        }
        forumMapper.refreshQuestionVoteCount(questionId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_voted", !voted);
        result.put("vote_count", intOrZero(forumMapper.findQuestionVoteCount(questionId)));
        return result;
    }

    @Transactional
    public Map<String, Object> favoriteQuestion(Long userId, Long questionId) {
        boolean favorited = forumMapper.countQuestionFavorite(userId, questionId) > 0;
        if (favorited) {
            forumMapper.deleteQuestionFavorite(userId, questionId);
        } else {
            forumMapper.insertQuestionFavorite(userId, questionId);
        }
        forumMapper.refreshQuestionFavoriteCount(questionId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_favorited", !favorited);
        result.put("favorite_count", intOrZero(forumMapper.findQuestionFavoriteCount(questionId)));
        return result;
    }

    @Transactional
    public Map<String, Object> followQuestion(Long userId, Long questionId) {
        boolean followed = forumMapper.countQuestionFollow(userId, questionId) > 0;
        if (followed) {
            forumMapper.deleteQuestionFollow(userId, questionId);
        } else {
            forumMapper.insertQuestionFollow(userId, questionId);
        }
        forumMapper.refreshQuestionFollowCount(questionId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_followed", !followed);
        result.put("follow_count", intOrZero(forumMapper.findQuestionFollowCount(questionId)));
        return result;
    }

    @Transactional
    public Map<String, Object> voteAnswer(Long userId, Long answerId) {
        boolean voted = forumMapper.countAnswerVote(userId, answerId) > 0;
        if (voted) {
            forumMapper.deleteAnswerVote(userId, answerId);
        } else {
            forumMapper.insertAnswerVote(userId, answerId);
        }
        forumMapper.refreshAnswerVoteCount(answerId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_voted", !voted);
        result.put("vote_count", intOrZero(forumMapper.findAnswerVoteCount(answerId)));
        return result;
    }

    @Transactional
    public Map<String, Object> collectAnswer(Long userId, Long answerId) {
        boolean collected = forumMapper.countAnswerCollection(userId, answerId) > 0;
        if (collected) {
            forumMapper.deleteAnswerCollection(userId, answerId);
        } else {
            forumMapper.insertAnswerCollection(userId, answerId);
        }
        forumMapper.refreshAnswerCollectionCount(answerId);

        Map<String, Object> result = new HashMap<>();
        result.put("is_collected", !collected);
        result.put("collect_count", intOrZero(forumMapper.findAnswerCollectionCount(answerId)));
        return result;
    }

    public List<Map<String, Object>> getMyCollections(Long userId) {
        List<Map<String, Object>> collections = new ArrayList<>(forumMapper.findQuestionFavoritesByUser(userId));
        collections.forEach(item -> {
            item.put("type", "question");
            item.put("target", Map.of(
                "id", item.get("target_id"),
                "title", item.remove("target_title")
            ));
        });

        List<Map<String, Object>> answerCollections = forumMapper.findAnswerCollectionsByUser(userId);
        answerCollections.forEach(item -> {
            item.put("type", "answer");
            item.put("target", Map.of(
                    "id", item.get("target_id"),
                    "title", item.remove("target_title")
            ));
        });
        collections.addAll(answerCollections);
        collections.sort((left, right) -> String.valueOf(right.get("created_at")).compareTo(String.valueOf(left.get("created_at"))));
        return collections;
    }

    public Map<String, Object> getForumUser(Long targetUserId, Long currentUserId) {
        Map<String, Object> user = forumMapper.findForumUserById(targetUserId);
        if (user != null) {
            user.put("is_following", currentUserId != null && !currentUserId.equals(targetUserId)
                    && forumMapper.countUserFollow(currentUserId, targetUserId) > 0);
        }
        return user;
    }

    public List<Map<String, Object>> getUserQuestions(Long userId, Long currentUserId) {
        List<Map<String, Object>> questions = forumMapper.findQuestionsByUser(userId);
        questions.forEach(question -> {
            decorateQuestion(question);
            decorateQuestionInteraction(question, currentUserId);
        });
        return questions;
    }

    /**
     * 分页获取某用户发布的问题。
     */
    public PageResult<Map<String, Object>> getUserQuestionsPage(Long userId, Long currentUserId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;
        long total = forumMapper.countQuestionsByUser(userId);
        List<Map<String, Object>> questions = forumMapper.findQuestionsByUserPaged(userId, offset, safeSize);
        questions.forEach(q -> {
            decorateQuestion(q);
            decorateQuestionInteraction(q, currentUserId);
        });
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    public List<Map<String, Object>> getUserAnswers(Long userId, Long currentUserId) {
        List<Map<String, Object>> answers = forumMapper.findAnswersByUser(userId);
        answers.forEach(answer -> {
            Object questionTitle = answer.remove("question_title");
            answer.put("question", Map.of(
                    "id", answer.get("question_id"),
                    "title", questionTitle == null ? "" : questionTitle
            ));
            decorateAuthor(answer);
            decorateAnswerInteraction(answer, currentUserId);
        });
        return answers;
    }

    @Transactional
    public Map<String, Object> followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("is_following", false);
            result.put("follower_count", intValue(forumMapper.findForumUserById(followingId).get("follower_count")));
            result.put("following_count", intValue(forumMapper.findForumUserById(followerId).get("following_count")));
            return result;
        }

        boolean following = forumMapper.countUserFollow(followerId, followingId) > 0;
        if (following) {
            forumMapper.deleteUserFollow(followerId, followingId);
        } else {
            forumMapper.insertUserFollow(followerId, followingId);
        }

        Map<String, Object> targetUser = forumMapper.findForumUserById(followingId);
        Map<String, Object> currentUser = forumMapper.findForumUserById(followerId);
        Map<String, Object> result = new HashMap<>();
        result.put("is_following", !following);
        result.put("follower_count", intValue(targetUser.get("follower_count")));
        result.put("following_count", intValue(currentUser.get("following_count")));
        return result;
    }

    public List<Map<String, Object>> getFollowingUsers(Long userId) {
        return forumMapper.findFollowingUsers(userId);
    }

    public List<Map<String, Object>> getFollowerUsers(Long userId) {
        return forumMapper.findFollowerUsers(userId);
    }

    public List<Map<String, Object>> getFollowedTopics(Long userId) {
        return forumMapper.findFollowedTopicsByUser(userId);
    }

    public List<Map<String, Object>> getFollowedQuestions(Long userId) {
        List<Map<String, Object>> questions = forumMapper.findFollowedQuestionsByUser(userId);
        questions.forEach(question -> {
            decorateQuestion(question);
            decorateQuestionInteraction(question, userId);
        });
        return questions;
    }

    /**
     * 分页获取当前用户关注的问题。
     */
    public PageResult<Map<String, Object>> getFollowedQuestionsPage(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;
        long total = forumMapper.countFollowedQuestionsByUser(userId);
        List<Map<String, Object>> questions = forumMapper.findFollowedQuestionsByUserPaged(userId, offset, safeSize);
        questions.forEach(q -> {
            decorateQuestion(q);
            decorateQuestionInteraction(q, userId);
        });
        return new PageResult<>(questions, total, safePage, safeSize);
    }

    /**
     * 分页搜索：按关键字搜索问题/用户/话题。
     */
    public Map<String, Object> search(String keyword, String type, String sort, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        int offset = (safePage - 1) * safeSize;
        String kw = keyword == null ? "" : keyword.trim();

        Map<String, Object> result = new HashMap<>();
        if (kw.isEmpty()) {
            result.put("questions", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
            result.put("users", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
            result.put("topics", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
            return result;
        }

        boolean all = type == null || type.isBlank() || "all".equals(type);
        String safeSort = "time".equalsIgnoreCase(sort) ? "time" : "relevance";
        if (all || "question".equals(type)) {
            long total = forumMapper.countSearchQuestions(kw);
            List<Map<String, Object>> records = forumMapper.searchQuestions(kw, safeSort, offset, safeSize);
            records.forEach(this::decorateQuestion);
            result.put("questions", new PageResult<>(records, total, safePage, safeSize));
        } else {
            result.put("questions", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
        }
        if (all || "user".equals(type)) {
            long total = forumMapper.countSearchUsers(kw);
            List<Map<String, Object>> records = forumMapper.searchUsers(kw, safeSort, offset, safeSize);
            result.put("users", new PageResult<>(records, total, safePage, safeSize));
        } else {
            result.put("users", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
        }
        if (all || "topic".equals(type)) {
            long total = forumMapper.countSearchTopics(kw);
            List<Map<String, Object>> records = forumMapper.searchTopics(kw, safeSort, offset, safeSize);
            result.put("topics", new PageResult<>(records, total, safePage, safeSize));
        } else {
            result.put("topics", new PageResult<>(new ArrayList<>(), 0, safePage, safeSize));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> createComment(Long userId, Map<String, Object> request) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("answerId", longValue(request.get("answer_id")));
        comment.put("userId", userId);
        comment.put("parentId", request.get("parent_id") == null ? null : longValue(request.get("parent_id")));
        comment.put("content", request.get("content"));
        forumMapper.insertComment(comment);
        return comment;
    }

    public List<Map<String, Object>> getComments(Long answerId) {
        List<Map<String, Object>> rows = forumMapper.findCommentsByAnswer(answerId);
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        rows.forEach(comment -> {
            decorateAuthor(comment);
            comment.put("replies", new ArrayList<Map<String, Object>>());
            comment.put("is_voted", false);
            byId.put(longValue(comment.get("id")), comment);
        });

        rows.forEach(comment -> {
            Object parentIdValue = comment.get("parent_id");
            if (parentIdValue == null) {
                roots.add(comment);
                return;
            }

            Map<String, Object> parent = byId.get(longValue(parentIdValue));
            Map<String, Object> root = findRootComment(comment, byId);
            if (parent == null || root == null) {
                roots.add(comment);
                return;
            }

            comment.remove("replies");
            comment.put("parent", Map.of("id", parent.get("id"), "author", parent.get("author")));
            getReplyList(root).add(comment);
        });

        return roots;
    }

    public Map<String, Object> voteComment(Long commentId) {
        forumMapper.increaseCommentVote(commentId);
        Integer voteCount = forumMapper.findCommentVoteCount(commentId);
        Map<String, Object> result = new HashMap<>();
        result.put("is_voted", true);
        result.put("vote_count", voteCount == null ? 0 : voteCount);
        return result;
    }

    private Map<String, Object> createTopicInternal(String name, String description) {
        Map<String, Object> topic = new HashMap<>();
        topic.put("name", name);
        topic.put("description", description);
        forumMapper.insertTopic(topic);
        return forumMapper.findTopicById(longValue(topic.get("id")));
    }

    private void ensureStatusColumns() {
        if (!columnExists("forum_topic", "status")) {
            forumMapper.addTopicStatusColumn();
        }
        if (!columnExists("forum_question", "status")) {
            forumMapper.addQuestionStatusColumn();
        }
        if (!columnExists("forum_answer", "status")) {
            forumMapper.addAnswerStatusColumn();
        }
        if (!columnExists("forum_comment", "status")) {
            forumMapper.addCommentStatusColumn();
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private String normalizeContentStatus(String status) {
        if ("active".equals(status) || "hidden".equals(status)) {
            return status;
        }
        throw new IllegalArgumentException("Invalid content status");
    }

    private void decorateAdminContentAuthor(Map<String, Object> row) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", row.remove("author_id"));
        author.put("username", row.remove("author_username"));
        author.put("avatar", row.remove("author_avatar"));
        row.put("author", author);
    }

    private void decorateQuestion(Map<String, Object> question) {
        Long questionId = longValue(question.get("id"));
        question.put("topics", forumMapper.findTopicsByQuestion(questionId));
        question.put("is_favorited", false);
        question.put("is_voted", false);
        question.put("is_followed", false);
        decorateAuthor(question);
    }

    private void decorateTopicInteraction(Map<String, Object> topic, Long userId) {
        if (userId == null) {
            topic.put("is_followed", false);
            return;
        }
        topic.put("is_followed", forumMapper.countTopicFollow(userId, longValue(topic.get("id"))) > 0);
    }

    private void decorateQuestionInteraction(Map<String, Object> question, Long userId) {
        if (userId == null) {
            return;
        }
        Long questionId = longValue(question.get("id"));
        question.put("is_favorited", forumMapper.countQuestionFavorite(userId, questionId) > 0);
        question.put("is_voted", forumMapper.countQuestionVote(userId, questionId) > 0);
        question.put("is_followed", forumMapper.countQuestionFollow(userId, questionId) > 0);
    }

    private void decorateAnswerInteraction(Map<String, Object> answer, Long userId) {
        if (userId == null) {
            return;
        }
        Long answerId = longValue(answer.get("id"));
        answer.put("is_voted", forumMapper.countAnswerVote(userId, answerId) > 0);
        answer.put("is_collected", forumMapper.countAnswerCollection(userId, answerId) > 0);
    }

    private void decorateAuthor(Map<String, Object> row) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", row.remove("author_id"));
        author.put("username", row.remove("author_username"));
        author.put("avatar", row.remove("author_avatar"));
        author.put("is_following", false);
        row.put("author", author);
        row.put("comment_count", row.getOrDefault("comment_count", 0));
        row.put("is_collected", false);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getReplyList(Map<String, Object> comment) {
        return (List<Map<String, Object>>) comment.get("replies");
    }

    private Map<String, Object> findRootComment(Map<String, Object> comment, Map<Long, Map<String, Object>> byId) {
        Map<String, Object> current = comment;
        while (current != null && current.get("parent_id") != null) {
            current = byId.get(longValue(current.get("parent_id")));
        }
        return current;
    }

    private List<Long> extractTopicIds(Object topicIds) {
        if (!(topicIds instanceof List<?> ids)) {
            return List.of();
        }

        return ids.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }

    private void sortByHotScore(List<Map<String, Object>> questions) {
        questions.sort((left, right) -> {
            int scoreCompare = Double.compare(hotScore(right), hotScore(left));
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return String.valueOf(right.get("created_at")).compareTo(String.valueOf(left.get("created_at")));
        });
    }

    private void sortByRecommendScore(List<Map<String, Object>> questions, Long userId) {
        Set<Long> preferredTopicIds = collectPreferredTopicIds(userId);
        if (preferredTopicIds.isEmpty()) {
            sortByHotScore(questions);
            return;
        }

        questions.sort((left, right) -> {
            int scoreCompare = Double.compare(
                    recommendationScore(right, preferredTopicIds),
                    recommendationScore(left, preferredTopicIds)
            );
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return String.valueOf(right.get("created_at")).compareTo(String.valueOf(left.get("created_at")));
        });
    }

    private double recommendationScore(Map<String, Object> question, Set<Long> preferredTopicIds) {
        double score = hotScore(question);
        for (Long topicId : questionTopicIds(question)) {
            if (preferredTopicIds.contains(topicId)) {
                score += 50;
            }
        }
        if (Boolean.TRUE.equals(question.get("is_favorited"))) {
            score += 20;
        }
        if (Boolean.TRUE.equals(question.get("is_followed"))) {
            score += 16;
        }
        return score;
    }

    private double hotScore(Map<String, Object> question) {
        return intValue(question.get("answer_count")) * 10.0
                + intValue(question.get("vote_count")) * 8.0
                + intValue(question.get("favorite_count")) * 7.0
                + intValue(question.get("follow_count")) * 6.0
                + intValue(question.get("view_count")) * 1.0;
    }

    private Set<Long> collectPreferredTopicIds(Long userId) {
        Set<Long> topicIds = new HashSet<>();
        if (userId == null) {
            return topicIds;
        }

        forumMapper.findFollowedTopicsByUser(userId).forEach(topic -> topicIds.add(longValue(topic.get("id"))));
        forumMapper.findQuestionFavoritesByUser(userId).forEach(item ->
                topicIds.addAll(forumMapper.findTopicsByQuestion(longValue(item.get("id"))).stream()
                        .map(topic -> longValue(topic.get("id")))
                        .toList())
        );
        return topicIds;
    }

    @SuppressWarnings("unchecked")
    private List<Long> questionTopicIds(Map<String, Object> question) {
        Object topicsValue = question.get("topics");
        if (!(topicsValue instanceof List<?> topics)) {
            return List.of();
        }

        return topics.stream()
                .filter(Map.class::isInstance)
                .map(topic -> longValue(((Map<String, Object>) topic).get("id")))
                .toList();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void recordQuestionView(Long questionId, Long userId, String sessionId) {
        String viewKey;
        if (userId != null) {
            viewKey = "user:" + userId;
        } else if (sessionId != null && !sessionId.isBlank()) {
            viewKey = "session:" + sessionId;
        } else {
            return;
        }
        forumMapper.insertQuestionView(questionId, viewKey, userId, sessionId);
        forumMapper.refreshQuestionViewCount(questionId);
    }

    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("questionCount", forumMapper.countTotalQuestionsByUser(userId));
        stats.put("answerCount", forumMapper.countTotalAnswersByUser(userId));
        stats.put("voteCount", forumMapper.countTotalVotesReceivedByUser(userId));
        return stats;
    }

    private String limitText(Object value, int maxLength) {
        String text = String.valueOf(value == null ? "" : value).trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
