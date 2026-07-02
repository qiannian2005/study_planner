package com.studyplanner.controller;

import com.studyplanner.dto.ApiResponse;
import com.studyplanner.service.ChatRoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatRoomService chatRoomService;

    public ChatController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/messages")
    public ApiResponse<Map<String, Object>> getMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "global") String roomType,
            @RequestParam(required = false) Long roomId,
            HttpSession session
    ) {
        return ApiResponse.success(chatRoomService.getMessages(page, pageSize, before, currentUserId(session), roomType, roomId));
    }

    @GetMapping("/online-users")
    public ApiResponse<List<Map<String, Object>>> getOnlineUsers() {
        return ApiResponse.success(chatRoomService.getOnlineUsers());
    }

    private Long currentUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
