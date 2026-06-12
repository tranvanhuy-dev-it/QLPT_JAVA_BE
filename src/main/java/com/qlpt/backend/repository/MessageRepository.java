package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ChatRoom;
import com.qlpt.backend.entity.Message;
import com.qlpt.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);

    long countByChatRoomAndSenderNotAndIsReadFalse(ChatRoom chatRoom, User sender);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.chatRoom = :chatRoom AND m.sender <> :user AND m.isRead = false")
    void markMessagesAsRead(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);
}
