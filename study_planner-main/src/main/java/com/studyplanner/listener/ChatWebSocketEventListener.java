package com.studyplanner.listener;

import com.studyplanner.service.ChatRoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatWebSocketEventListener {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketEventListener(ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate) {
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Long userId = chatRoomService.resolveUserId(accessor.getSessionAttributes());
        Map<String, Object> user = chatRoomService.connect(accessor.getSessionId(), userId);
        if (user == null) {
            return;
        }

        List<Map<String, Object>> users = chatRoomService.getOnlineUsers();
        Map<String, Object> payload = new HashMap<>(user);
        payload.put("users", users);
        messagingTemplate.convertAndSend("/topic/chat", chatRoomService.event("user_joined", payload));
        messagingTemplate.convertAndSend("/topic/chat", chatRoomService.event("online_users", users));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Map<String, Object> user = chatRoomService.disconnect(event.getSessionId());
        if (user == null) {
            return;
        }

        List<Map<String, Object>> users = chatRoomService.getOnlineUsers();
        Map<String, Object> payload = new HashMap<>(user);
        payload.put("users", users);
        messagingTemplate.convertAndSend("/topic/chat", chatRoomService.event("user_left", payload));
        messagingTemplate.convertAndSend("/topic/chat", chatRoomService.event("online_users", users));
    }
}
