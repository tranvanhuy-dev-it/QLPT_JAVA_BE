package com.qlpt.backend.service;

import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.ExtraFee;
import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.RoomStatus;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.BoardingHouseRepository;
import com.qlpt.backend.repository.ExtraFeeRepository;
import com.qlpt.backend.repository.RoomRepository;
import com.qlpt.backend.repository.ContractExtraFeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final BoardingHouseRepository boardingHouseRepository;
    private final RoomRepository roomRepository;
    private final ExtraFeeRepository extraFeeRepository;
    private final ContractExtraFeeRepository contractExtraFeeRepository;

    public RoomService(BoardingHouseRepository boardingHouseRepository,
                       RoomRepository roomRepository,
                       ExtraFeeRepository extraFeeRepository,
                       ContractExtraFeeRepository contractExtraFeeRepository) {
        this.boardingHouseRepository = boardingHouseRepository;
        this.roomRepository = roomRepository;
        this.extraFeeRepository = extraFeeRepository;
        this.contractExtraFeeRepository = contractExtraFeeRepository;
    }

    // ==========================================
    // BOARDING HOUSE METHODS
    // ==========================================

    @Transactional
    public BoardingHouse createBoardingHouse(BoardingHouse boardingHouse, User landlord) {
        boardingHouse.setLandlord(landlord);
        if (boardingHouse.getExtraFees() != null) {
            for (ExtraFee ef : boardingHouse.getExtraFees()) {
                ef.setBoardingHouse(boardingHouse);
            }
        }
        return boardingHouseRepository.save(boardingHouse);
    }

    @Transactional(readOnly = true)
    public Page<BoardingHouse> getBoardingHousesByLandlord(User landlord, Pageable pageable) {
        return boardingHouseRepository.findByLandlordId(landlord.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public BoardingHouse getBoardingHouseById(UUID id, User landlord) {
        BoardingHouse boardingHouse = boardingHouseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dãy trọ"));
        
        if (!boardingHouse.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập dãy trọ này");
        }
        return boardingHouse;
    }

    @Transactional
    public BoardingHouse updateBoardingHouse(UUID id, BoardingHouse bhDetails, User landlord) {
        BoardingHouse boardingHouse = getBoardingHouseById(id, landlord);
        
        boardingHouse.setName(bhDetails.getName());
        boardingHouse.setAddress(bhDetails.getAddress());
        boardingHouse.setDefaultElectricityRate(bhDetails.getDefaultElectricityRate());
        boardingHouse.setDefaultWaterRate(bhDetails.getDefaultWaterRate());
        boardingHouse.setWaterBillingType(bhDetails.getWaterBillingType());
        boardingHouse.setBankName(bhDetails.getBankName());
        boardingHouse.setBankAccountNumber(bhDetails.getBankAccountNumber());
        boardingHouse.setBankAccountName(bhDetails.getBankAccountName());
        boardingHouse.setRules(bhDetails.getRules());
        boardingHouse.setFixedBillingDay(bhDetails.getFixedBillingDay());
        
        // Cập nhật phụ phí dịch vụ qua JPA Cascade
        boardingHouse.getExtraFees().clear();
        if (bhDetails.getExtraFees() != null) {
            for (ExtraFee ef : bhDetails.getExtraFees()) {
                ef.setBoardingHouse(boardingHouse);
                boardingHouse.getExtraFees().add(ef);
            }
        }
        
        return boardingHouseRepository.save(boardingHouse);
    }

    @Transactional
    public void deleteBoardingHouse(UUID id, User landlord) {
        BoardingHouse boardingHouse = getBoardingHouseById(id, landlord);
        boardingHouseRepository.delete(boardingHouse);
    }

    // ==========================================
    // ROOM METHODS
    // ==========================================

    @Transactional
    public Room createRoom(UUID boardingHouseId, Room room, User landlord) {
        BoardingHouse boardingHouse = getBoardingHouseById(boardingHouseId, landlord);
        room.setBoardingHouse(boardingHouse);
        room.setStatus(RoomStatus.VACANT);
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public Page<Room> getRoomsByBoardingHouse(UUID boardingHouseId, User landlord, Pageable pageable) {
        // Validation check for landlord access
        getBoardingHouseById(boardingHouseId, landlord);
        return roomRepository.findByBoardingHouseId(boardingHouseId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Room> getRoomsByLandlord(User landlord, Pageable pageable) {
        return roomRepository.findByBoardingHouseLandlordId(landlord.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Room getRoomById(UUID roomId, User landlord) {
        Room room = roomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng trọ"));
        
        if (!room.getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập phòng trọ này");
        }
        return room;
    }

    @Transactional
    public Room updateRoom(UUID roomId, Room roomDetails, User landlord) {
        Room room = getRoomById(roomId, landlord);
        
        room.setRoomNumber(roomDetails.getRoomNumber());
        room.setBasePrice(roomDetails.getBasePrice());
        room.setMaxPeople(roomDetails.getMaxPeople());
        
        // Cập nhật chỉ số đồng hồ điện nước ban đầu hoặc hiện tại
        room.setCurrentElectricityIndex(roomDetails.getCurrentElectricityIndex());
        room.setCurrentWaterIndex(roomDetails.getCurrentWaterIndex());
        
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(UUID roomId, User landlord) {
        Room room = getRoomById(roomId, landlord);
        roomRepository.delete(room);
    }

    // ==========================================
    // EXTRA FEE METHODS
    // ==========================================

    @Transactional
    public ExtraFee createExtraFee(UUID boardingHouseId, ExtraFee extraFee, User landlord) {
        BoardingHouse boardingHouse = getBoardingHouseById(boardingHouseId, landlord);
        extraFee.setBoardingHouse(boardingHouse);
        return extraFeeRepository.save(extraFee);
    }

    @Transactional(readOnly = true)
    public List<ExtraFee> getExtraFeesByBoardingHouse(UUID boardingHouseId, User landlord) {
        getBoardingHouseById(boardingHouseId, landlord);
        return extraFeeRepository.findByBoardingHouseId(boardingHouseId);
    }

    @Transactional
    public void deleteExtraFee(UUID extraFeeId, User landlord) {
        ExtraFee extraFee = extraFeeRepository.findById(extraFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ phí này"));
        
        if (!extraFee.getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa phụ phí này");
        }
        // Delete all associated contract extra fees to prevent FK violations
        contractExtraFeeRepository.deleteByExtraFeeId(extraFeeId);
        extraFeeRepository.delete(extraFee);
    }
}
