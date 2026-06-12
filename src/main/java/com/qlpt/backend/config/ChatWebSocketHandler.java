package com.qlpt.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenFromSession(session);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("Từ chối kết nối WebSocket Chat: Token không hợp lệ hoặc bị thiếu.");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String username = jwtTokenProvider.getUsernameFromJWT(token);
        if (username == null) {
            log.warn("Từ chối kết nối WebSocket Chat: Không thể trích xuất tên người dùng từ token.");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(session);
        session.getAttributes().put("username", username);
        
        log.info("Kết nối WebSocket Chat được thiết lập thành công cho người dùng: {}", username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            Set<WebSocketSession> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                }
            }
            log.info("Kết nối WebSocket Chat của người dùng {} đã bị đóng. Trạng thái: {}", username, status);
        }
    }

    public void sendMessageToUser(String username, Object payload) {
        Set<WebSocketSession> sessions = userSessions.get(username);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("Người dùng {} không hoạt động trực tuyến. Không thể đẩy tin nhắn qua WebSocket.", username);
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        log.info("Đã đẩy tin nhắn thành công cho {} qua session ID: {}", username, session.getId());
                    } catch (IOException e) {
                        log.error("Lỗi khi đẩy tin nhắn cho " + username + " qua session " + session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi tin nhắn sang JSON cho người dùng: " + username, e);
        }
    }

    private String getTokenFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        
        String query = uri.getQuery();
        if (query == null) return null;

        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length > 1 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
