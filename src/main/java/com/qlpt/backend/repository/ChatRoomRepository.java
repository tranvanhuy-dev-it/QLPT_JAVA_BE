package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ChatRoom;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByContract(Contract contract);

    Optional<ChatRoom> findByRoomAndContract(Room room, Contract contract);

    @Query("SELECT cm.chatRoom FROM ChatRoomMember cm WHERE cm.user = :user ORDER BY cm.chatRoom.updatedAt DESC")
    Page<ChatRoom> findByUserOrderByUpdatedAtDesc(@Param("user") User user, Pageable pageable);
}
