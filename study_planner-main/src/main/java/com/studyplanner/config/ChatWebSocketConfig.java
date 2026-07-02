package com.studyplanner.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/chat/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), new ChatHandshakeInterceptor())
                .withSockJS();
    }

    private static class ChatHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes
        ) {
            Long userId = resolveUserIdFromSession(request);
            if (userId == null) {
                userId = resolveUserIdFromQuery(request);
            }
            if (userId != null) {
                attributes.put("userId", userId);
            }
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception
        ) {
        }

        private Long resolveUserIdFromSession(ServerHttpRequest request) {
            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                return null;
            }
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session == null) {
                return null;
            }
            return toLong(session.getAttribute("userId"));
        }

        private Long resolveUserIdFromQuery(ServerHttpRequest request) {
            MultiValueMap<String, String> queryParams = UriComponentsBuilder
                    .fromUri(request.getURI())
                    .build()
                    .getQueryParams();
            return toLong(queryParams.getFirst("token"));
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
    }
}
