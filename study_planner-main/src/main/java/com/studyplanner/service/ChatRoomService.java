package com.studyplanner.service;

import com.studyplanner.entity.User;
import com.studyplanner.mapper.ChatRoomMapper;
import com.studyplanner.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PlanGroupService planGroupService;
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    public ChatRoomService(
            ChatRoomMapper chatRoomMapper,
            UserMapper userMapper,
            JdbcTemplate jdbcTemplate,
            PlanGroupService planGroupService
    ) {
        this.chatRoomMapper = chatRoomMapper;
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.planGroupService = planGroupService;
    }

    @PostConstruct
    public void initializeSchema() {
        chatRoomMapper.createMessageTable();
        if (!columnExists("chat_room_message", "room_type")) {
            chatRoomMapper.addRoomTypeColumn();
        }
        if (!columnExists("chat_room_message", "room_id")) {
            chatRoomMapper.addRoomIdColumn();
        }
    }

    public Map<String, Object> getMessages(int page, int pageSize, String before, Long currentUserId) {
        return getMessages(page, pageSize, before, currentUserId, "global", null);
    }

    public Map<String, Object> getMessages(
            int page,
            int pageSize,
            String before,
            Long currentUserId,
            String roomType,
            Long roomId
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;
        RoomScope scope = resolveRoomScope(roomType, roomId, currentUserId);

        List<Map<String, Object>> rows = new ArrayList<>(
                chatRoomMapper.findMessages(before, scope.roomType(), scope.roomId(), safePageSize, offset)
        );
        Collections.reverse(rows);
        rows.forEach(row -> decorateOwnFlag(row, currentUserId));

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        result.put("total", chatRoomMapper.countMessages(before, scope.roomType(), scope.roomId()));
        return result;
    }

    public List<Map<String, Object>> getOnlineUsers() {
        List<Map<String, Object>> users = userSessions.keySet().stream()
                .map(this::getChatUser)
                .filter(user -> user != null)
                .sorted(Comparator.comparing(user -> String.valueOf(user.get("username"))))
                .toList();
        return new ArrayList<>(users);
    }

    @Transactional
    public Map<String, Object> createMessage(Long userId, String content) {
        return createMessage(userId, content, "global", null);
    }

    @Transactional
    public Map<String, Object> createMessage(Long userId, String content, String roomType, Long roomId) {
        Long safeUserId = requireUserId(userId);
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        RoomScope scope = resolveRoomScope(roomType, roomId, safeUserId);

        Map<String, Object> message = new HashMap<>();
        message.put("userId", safeUserId);
        message.put("roomType", scope.roomType());
        message.put("roomId", scope.roomId());
        message.put("content", safeContent);
        chatRoomMapper.insertMessage(message);

        Map<String, Object> saved = chatRoomMapper.findMessageById(toLong(message.get("id")));
        decorateOwnFlag(saved, safeUserId);
        return saved;
    }

    public Map<String, Object> connect(String sessionId, Long userId) {
        if (sessionId == null || userId == null || userMapper.findById(userId) == null) {
            return null;
        }
        sessionUsers.put(sessionId, userId);
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        return getChatUser(userId);
    }

    public Map<String, Object> disconnect(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        Long userId = sessionUsers.remove(sessionId);
        if (userId == null) {
            return null;
        }
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        return getChatUser(userId);
    }

    public Long resolveUserId(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            return null;
        }
        return toLong(sessionAttributes.get("userId"));
    }

    public Map<String, Object> event(String type, Object payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("payload", payload);
        return event;
    }

    private Map<String, Object> getChatUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("user_id", user.getId());
        result.put("username", user.getUsername());
        result.put("avatar", user.getAvatar());
        return result;
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userMapper.findById(userId) == null) {
            throw new IllegalArgumentException("Please log in first");
        }
        return userId;
    }

    private void decorateOwnFlag(Map<String, Object> row, Long currentUserId) {
        if (row == null) {
            return;
        }
        Long messageUserId = toLong(row.get("user_id"));
        row.put("is_own", currentUserId != null && currentUserId.equals(messageUserId));
    }

    private RoomScope resolveRoomScope(String roomType, Long roomId, Long userId) {
        String safeRoomType = roomType == null || roomType.trim().isEmpty() ? "global" : roomType.trim();
        if ("plan".equals(safeRoomType)) {
            if (roomId == null) {
                throw new IllegalArgumentException("Plan room id is required");
            }
            if (!planGroupService.canAccessPlan(roomId, userId)) {
                throw new RuntimeException("No permission to access this plan chat");
            }
            return new RoomScope("plan", roomId);
        }
        return new RoomScope("global", null);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record RoomScope(String roomType, Long roomId) {
    }
}
