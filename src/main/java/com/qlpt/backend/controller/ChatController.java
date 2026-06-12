package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.chat.ChatRoomResponse;
import com.qlpt.backend.dto.chat.MessageRequest;
import com.qlpt.backend.dto.chat.MessageResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/rooms")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = userDetails.getUser();
        Page<ChatRoomResponse> rooms = chatService.getChatRooms(user, pageable);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<MessageResponse>> getRoomMessages(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 30) Pageable pageable) {
        User user = userDetails.getUser();
        Page<MessageResponse> messages = chatService.getRoomMessages(roomId, user, pageable);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MessageRequest request) {
        User user = userDetails.getUser();
        MessageResponse response = chatService.sendMessage(roomId, user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        chatService.markAsRead(roomId, user);
        return ResponseEntity.ok().build();
    }
}
