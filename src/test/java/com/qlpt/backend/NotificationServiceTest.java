package com.qlpt.backend;

import com.qlpt.backend.dto.notification.NotificationResponse;
import com.qlpt.backend.entity.Notification;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.NotificationRepository;
import com.qlpt.backend.service.NotificationService;
import com.qlpt.backend.service.impl.NotificationServiceImpl;
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

    @Mock
    private com.qlpt.backend.repository.UserRepository userRepository;

    @Mock
    private com.qlpt.backend.config.NotificationWebSocketHandler notificationWebSocketHandler;

    @InjectMocks
    private NotificationServiceImpl notificationService;

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

    @Test
    public void testSendNotificationToAllTenants() {
        // GIVEN
        User landlord = User.builder().id(UUID.randomUUID()).username("landlord").build();
        User tenant1 = User.builder().id(UUID.randomUUID()).username("tenant1").build();
        User tenant2 = User.builder().id(UUID.randomUUID()).username("tenant2").build();
        java.util.List<User> tenants = java.util.Arrays.asList(tenant1, tenant2);

        when(userRepository.findByRoleAndLandlordId(com.qlpt.backend.enums.Role.TENANT, landlord.getId()))
                .thenReturn(tenants);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        notificationService.sendNotificationToAllTenants(landlord, "Hello", "Announcement");

        // THEN
        verify(userRepository, times(1)).findByRoleAndLandlordId(com.qlpt.backend.enums.Role.TENANT, landlord.getId());
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationWebSocketHandler, times(2)).sendNotificationToUser(any(String.class), any(NotificationResponse.class));
    }
}
