package com.qlpt.backend.service;

import com.qlpt.backend.dto.chat.ChatRoomResponse;
import com.qlpt.backend.dto.chat.MessageRequest;
import com.qlpt.backend.dto.chat.MessageResponse;
import com.qlpt.backend.entity.ChatRoom;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    ChatRoom getOrCreateChatRoom(Contract contract);

    Page<ChatRoomResponse> getChatRooms(User user, Pageable pageable);

    Page<MessageResponse> getRoomMessages(UUID roomId, User user, Pageable pageable);

    MessageResponse sendMessage(UUID roomId, User user, MessageRequest request);

    void markAsRead(UUID roomId, User user);
}
