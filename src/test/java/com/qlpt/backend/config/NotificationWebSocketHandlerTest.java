package com.qlpt.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlpt.backend.dto.notification.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationWebSocketHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private WebSocketSession session;

    private ObjectMapper objectMapper;
    private NotificationWebSocketHandler handler;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        handler = new NotificationWebSocketHandler(jwtTokenProvider, objectMapper);
    }

    @Test
    public void testConnectionEstablished_Success() throws Exception {
        // GIVEN
        URI uri = new URI("ws://localhost:8080/ws/notifications?token=valid-jwt-token");
        when(session.getUri()).thenReturn(uri);
        when(jwtTokenProvider.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromJWT("valid-jwt-token")).thenReturn("testuser");
        
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);

        // WHEN
        handler.afterConnectionEstablished(session);

        // THEN
        verify(session, never()).close(any(CloseStatus.class));
        assertEquals("testuser", attributes.get("username"));
    }

    @Test
    public void testConnectionEstablished_InvalidToken_ClosesSession() throws Exception {
        // GIVEN
        URI uri = new URI("ws://localhost:8080/ws/notifications?token=invalid-jwt-token");
        when(session.getUri()).thenReturn(uri);
        when(jwtTokenProvider.validateToken("invalid-jwt-token")).thenReturn(false);

        // WHEN
        handler.afterConnectionEstablished(session);

        // THEN
        verify(session, times(1)).close(CloseStatus.BAD_DATA);
    }

    @Test
    public void testConnectionEstablished_NoToken_ClosesSession() throws Exception {
        // GIVEN
        URI uri = new URI("ws://localhost:8080/ws/notifications");
        when(session.getUri()).thenReturn(uri);

        // WHEN
        handler.afterConnectionEstablished(session);

        // THEN
        verify(session, times(1)).close(CloseStatus.BAD_DATA);
    }

    @Test
    public void testSendNotificationToUser_Success() throws Exception {
        // GIVEN: Establish connection
        URI uri = new URI("ws://localhost:8080/ws/notifications?token=valid-token");
        when(session.getUri()).thenReturn(uri);
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromJWT("valid-token")).thenReturn("testuser");
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        handler.afterConnectionEstablished(session);

        // Notify
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "Test Title", "Test Content", "INFO", null, false, LocalDateTime.now()
        );
        when(session.isOpen()).thenReturn(true);

        // WHEN
        handler.sendNotificationToUser("testuser", response);

        // THEN
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(messageCaptor.capture());
        
        String sentPayload = messageCaptor.getValue().getPayload();
        assertTrue(sentPayload.contains("Test Title"));
        assertTrue(sentPayload.contains("Test Content"));
    }

    @Test
    public void testAfterConnectionClosed_RemovesSession() throws Exception {
        // GIVEN: Establish connection
        URI uri = new URI("ws://localhost:8080/ws/notifications?token=valid-token");
        when(session.getUri()).thenReturn(uri);
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromJWT("valid-token")).thenReturn("testuser");
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        handler.afterConnectionEstablished(session);

        // Close connection
        // WHEN
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // THEN: send notification should not attempt to send to closed session
        reset(session); // Reset mocks to verify no further interactions
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "Test Title", "Test Content", "INFO", null, false, LocalDateTime.now()
        );
        handler.sendNotificationToUser("testuser", response);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
