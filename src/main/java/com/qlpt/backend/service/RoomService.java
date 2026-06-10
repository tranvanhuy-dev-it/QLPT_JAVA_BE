package com.qlpt.backend.service;

import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.ExtraFee;
import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.RoomStatus;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.BoardingHouseRepository;
import com.qlpt.backend.repository.ContractExtraFeeRepository;
import com.qlpt.backend.repository.ExtraFeeRepository;
import com.qlpt.backend.repository.RoomRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface RoomService {
    public BoardingHouse createBoardingHouse(BoardingHouse boardingHouse, User landlord);
    public Page<BoardingHouse> getBoardingHousesByLandlord(User landlord, Pageable pageable);
    public BoardingHouse getBoardingHouseById(UUID id, User landlord);
    public BoardingHouse updateBoardingHouse(UUID id, BoardingHouse bhDetails, User landlord);
    public void deleteBoardingHouse(UUID id, User landlord);
    public Room createRoom(UUID boardingHouseId, Room room, User landlord);
    public Page<Room> getRoomsByBoardingHouse(UUID boardingHouseId, User landlord, Pageable pageable);
    public Page<Room> getRoomsByLandlord(User landlord, Pageable pageable);
    public Room getRoomById(UUID roomId, User landlord);
    public Room updateRoom(UUID roomId, Room roomDetails, User landlord);
    public void deleteRoom(UUID roomId, User landlord);
    public ExtraFee createExtraFee(UUID boardingHouseId, ExtraFee extraFee, User landlord);
    public List<ExtraFee> getExtraFeesByBoardingHouse(UUID boardingHouseId, User landlord);
    public void deleteExtraFee(UUID extraFeeId, User landlord);
}
