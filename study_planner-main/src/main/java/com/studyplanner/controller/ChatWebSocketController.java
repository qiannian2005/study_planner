package com.studyplanner.controller;

import com.studyplanner.service.ChatRoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate) {
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/message")
    public void sendMessage(Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = chatRoomService.resolveUserId(headerAccessor.getSessionAttributes());
        String content = extractContent(request);
        String roomType = extractString(request, "roomType", "global");
        Long roomId = extractLong(request, "roomId");
        Map<String, Object> message = chatRoomService.createMessage(userId, content, roomType, roomId);
        messagingTemplate.convertAndSend("/topic/chat", chatRoomService.event("message", message));
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> request) {
        Object payload = request.get("payload");
        if (payload instanceof Map<?, ?> payloadMap) {
            Object content = ((Map<String, Object>) payloadMap).get("content");
            return content == null ? "" : String.valueOf(content);
        }
        Object content = request.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    @SuppressWarnings("unchecked")
    private String extractString(Map<String, Object> request, String key, String defaultValue) {
        Object payload = request.get("payload");
        if (payload instanceof Map<?, ?> payloadMap) {
            Object value = ((Map<String, Object>) payloadMap).get(key);
            return value == null ? defaultValue : String.valueOf(value);
        }
        Object value = request.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Long extractLong(Map<String, Object> request, String key) {
        Object payload = request.get("payload");
        Object value = null;
        if (payload instanceof Map<?, ?> payloadMap) {
            value = ((Map<String, Object>) payloadMap).get(key);
        }
        if (value == null) {
            value = request.get(key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value).trim());
    }
}
