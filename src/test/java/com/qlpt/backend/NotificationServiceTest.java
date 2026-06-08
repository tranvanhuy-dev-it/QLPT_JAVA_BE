package com.qlpt.backend;

import com.qlpt.backend.dto.NotificationResponse;
import com.qlpt.backend.entity.Notification;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.NotificationRepository;
import com.qlpt.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .fullName("Test User")
                .build();
    }

    @Test
    public void testCreateNotification_Success() {
        // GIVEN
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // WHEN
        NotificationResponse response = notificationService.createNotification(user, "Title", "Content", "INFO");

        // THEN
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Title", response.title());
        assertEquals("Content", response.content());
        assertEquals("INFO", response.type());
        assertFalse(response.isRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testCreateNotification_NullUser_ThrowsException() {
        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.createNotification(null, "Title", "Content", "INFO");
        });
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    public void testDeleteNotificationsOlderThan() {
        // GIVEN
        when(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(5);

        // WHEN
        int deletedCount = notificationService.deleteNotificationsOlderThan(30);

        // THEN
        assertEquals(5, deletedCount);
        verify(notificationRepository, times(1)).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
