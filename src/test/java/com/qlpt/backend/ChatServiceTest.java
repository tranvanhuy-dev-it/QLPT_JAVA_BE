package com.qlpt.backend;

import com.qlpt.backend.config.ChatWebSocketHandler;
import com.qlpt.backend.dto.chat.ChatRoomResponse;
import com.qlpt.backend.dto.chat.MessageRequest;
import com.qlpt.backend.dto.chat.MessageResponse;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.ContractStatus;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.repository.*;
import com.qlpt.backend.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatWebSocketHandler chatWebSocketHandler;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User landlord;
    private User tenant;
    private Room room;
    private Contract contract;
    private ChatRoom chatRoom;

    @BeforeEach
    public void setUp() {
        landlord = User.builder()
                .id(UUID.randomUUID())
                .username("landlord")
                .fullName("Chủ trọ")
                .role(Role.LANDLORD)
                .build();

        tenant = User.builder()
                .id(UUID.randomUUID())
                .username("tenant")
                .fullName("Người thuê")
                .role(Role.TENANT)
                .build();

        BoardingHouse bh = BoardingHouse.builder()
                .id(UUID.randomUUID())
                .name("Nhà trọ A")
                .landlord(landlord)
                .build();

        room = Room.builder()
                .id(UUID.randomUUID())
                .roomNumber("101")
                .boardingHouse(bh)
                .build();

        contract = Contract.builder()
                .id(UUID.randomUUID())
                .room(room)
                .tenant(tenant)
                .status(ContractStatus.ACTIVE)
                .build();

        chatRoom = ChatRoom.builder()
                .id(UUID.randomUUID())
                .room(room)
                .contract(contract)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testGetOrCreateChatRoom_CreateNew() {
        // GIVEN
        when(chatRoomRepository.findChatRoomBetweenUsers(landlord, tenant)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        ChatRoom created = chatService.getOrCreateChatRoom(contract);

        // THEN
        assertNotNull(created);
        assertNull(created.getRoom());
        assertNull(created.getContract());
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    public void testGetOrCreateChatRoom_Existing() {
        // GIVEN
        when(chatRoomRepository.findChatRoomBetweenUsers(landlord, tenant)).thenReturn(Optional.of(chatRoom));

        // WHEN
        ChatRoom result = chatService.getOrCreateChatRoom(contract);

        // THEN
        assertEquals(chatRoom, result);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    public void testGetChatRooms_TenantSelfHealing() {
        // GIVEN
        Page<Contract> contractPage = new PageImpl<>(Collections.singletonList(contract));
        when(contractRepository.findByTenantIdAndStatus(tenant.getId(), ContractStatus.ACTIVE, PageRequest.of(0, 10)))
                .thenReturn(contractPage);
        when(chatRoomRepository.findChatRoomBetweenUsers(landlord, tenant)).thenReturn(Optional.of(chatRoom));

        Page<ChatRoom> roomPage = new PageImpl<>(Collections.singletonList(chatRoom));
        when(chatRoomRepository.findByUserOrderByUpdatedAtDesc(tenant, Pageable.unpaged())).thenReturn(roomPage);
        when(messageRepository.findByChatRoomOrderByCreatedAtDesc(any(ChatRoom.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(tenant).build();
        when(chatRoomMemberRepository.findByChatRoom(chatRoom)).thenReturn(Collections.singletonList(member));
        when(contractRepository.findByTenantIdAndStatus(tenant.getId(), ContractStatus.ACTIVE, PageRequest.of(0, 1)))
                .thenReturn(contractPage);

        // WHEN
        Page<ChatRoomResponse> responses = chatService.getChatRooms(tenant, Pageable.unpaged());

        // THEN
        assertFalse(responses.isEmpty());
        verify(contractRepository, times(1)).findByTenantIdAndStatus(tenant.getId(), ContractStatus.ACTIVE, PageRequest.of(0, 10));
        verify(chatRoomRepository, times(1)).findByUserOrderByUpdatedAtDesc(tenant, Pageable.unpaged());
    }

    @Test
    public void testSendMessage_Success() {
        // GIVEN
        MessageRequest request = new MessageRequest("Hello", "TEXT", null);
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, tenant)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        ChatRoomMember landlordMember = ChatRoomMember.builder().user(landlord).build();
        ChatRoomMember tenantMember = ChatRoomMember.builder().user(tenant).build();
        when(chatRoomMemberRepository.findByChatRoom(chatRoom)).thenReturn(java.util.Arrays.asList(landlordMember, tenantMember));

        // WHEN
        MessageResponse response = chatService.sendMessage(chatRoom.getId(), tenant, request);

        // THEN
        assertNotNull(response);
        assertEquals("Hello", response.content());
        assertEquals("TEXT", response.type());
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(chatWebSocketHandler, times(2)).sendMessageToUser(any(String.class), any(MessageResponse.class));
    }
}
