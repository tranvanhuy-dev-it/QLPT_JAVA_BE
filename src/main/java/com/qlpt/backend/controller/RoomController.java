package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseResponse;
import com.qlpt.backend.dto.extrafee.ExtraFeeResponse;
import com.qlpt.backend.dto.room.RoomResponse;
import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.ExtraFee;
import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@PreAuthorize("hasRole('LANDLORD')")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ==========================================
    // BOARDING HOUSE ENDPOINTS
    // ==========================================

    @PostMapping("/boarding-houses")
    public ResponseEntity<BoardingHouseResponse> createBoardingHouse(
            @RequestBody BoardingHouse boardingHouse,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouse created = roomService.createBoardingHouse(boardingHouse, landlord);
        return ResponseEntity.ok(BoardingHouseResponse.fromEntity(created));
    }

    @GetMapping("/boarding-houses")
    public ResponseEntity<Page<BoardingHouseResponse>> getBoardingHouses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        Page<BoardingHouse> houses = roomService.getBoardingHousesByLandlord(landlord, pageable);
        return ResponseEntity.ok(houses.map(BoardingHouseResponse::fromEntity));
    }

    @GetMapping("/boarding-houses/{id}")
    public ResponseEntity<BoardingHouseResponse> getBoardingHouse(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouse house = roomService.getBoardingHouseById(id, landlord);
        return ResponseEntity.ok(BoardingHouseResponse.fromEntity(house));
    }

    @PutMapping("/boarding-houses/{id}")
    public ResponseEntity<BoardingHouseResponse> updateBoardingHouse(
            @PathVariable UUID id,
            @RequestBody BoardingHouse boardingHouse,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouse updated = roomService.updateBoardingHouse(id, boardingHouse, landlord);
        return ResponseEntity.ok(BoardingHouseResponse.fromEntity(updated));
    }

    @DeleteMapping("/boarding-houses/{id}")
    public ResponseEntity<Void> deleteBoardingHouse(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        roomService.deleteBoardingHouse(id, landlord);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // ROOM ENDPOINTS
    // ==========================================

    @PostMapping("/boarding-houses/{bhId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(
            @PathVariable UUID bhId,
            @RequestBody Room room,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Room created = roomService.createRoom(bhId, room, landlord);
        return ResponseEntity.ok(RoomResponse.fromEntity(created));
    }

    @GetMapping("/boarding-houses/{bhId}/rooms")
    public ResponseEntity<Page<RoomResponse>> getRoomsByBoardingHouse(
            @PathVariable UUID bhId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        Page<Room> rooms = roomService.getRoomsByBoardingHouse(bhId, landlord, pageable);
        return ResponseEntity.ok(rooms.map(RoomResponse::fromEntityLight));
    }

    @GetMapping
    public ResponseEntity<Page<RoomResponse>> getAllRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        Page<Room> rooms = roomService.getRoomsByLandlord(landlord, pageable);
        return ResponseEntity.ok(rooms.map(RoomResponse::fromEntityLight));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Room room = roomService.getRoomById(id, landlord);
        return ResponseEntity.ok(RoomResponse.fromEntity(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable UUID id,
            @RequestBody Room room,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Room updated = roomService.updateRoom(id, room, landlord);
        return ResponseEntity.ok(RoomResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        roomService.deleteRoom(id, landlord);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // EXTRA FEE ENDPOINTS
    // ==========================================

    @PostMapping("/boarding-houses/{bhId}/extra-fees")
    public ResponseEntity<ExtraFeeResponse> createExtraFee(
            @PathVariable UUID bhId,
            @RequestBody ExtraFee extraFee,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        ExtraFee created = roomService.createExtraFee(bhId, extraFee, landlord);
        return ResponseEntity.ok(ExtraFeeResponse.fromEntity(created));
    }

    @GetMapping("/boarding-houses/{bhId}/extra-fees")
    public ResponseEntity<List<ExtraFeeResponse>> getExtraFees(
            @PathVariable UUID bhId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<ExtraFee> fees = roomService.getExtraFeesByBoardingHouse(bhId, landlord);
        List<ExtraFeeResponse> dtos = fees.stream()
                .map(ExtraFeeResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/extra-fees/{id}")
    public ResponseEntity<Void> deleteExtraFee(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        roomService.deleteExtraFee(id, landlord);
        return ResponseEntity.noContent().build();
    }
}
