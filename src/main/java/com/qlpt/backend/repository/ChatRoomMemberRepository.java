package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ChatRoom;
import com.qlpt.backend.entity.ChatRoomMember;
import com.qlpt.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);
}
