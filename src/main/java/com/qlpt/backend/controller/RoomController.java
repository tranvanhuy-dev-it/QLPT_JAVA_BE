package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
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
    public ResponseEntity<BoardingHouse> createBoardingHouse(
            @RequestBody BoardingHouse boardingHouse,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.createBoardingHouse(boardingHouse, landlord));
    }

    @GetMapping("/boarding-houses")
    public ResponseEntity<Page<BoardingHouse>> getBoardingHouses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getBoardingHousesByLandlord(landlord, pageable));
    }

    @GetMapping("/boarding-houses/{id}")
    public ResponseEntity<BoardingHouse> getBoardingHouse(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getBoardingHouseById(id, landlord));
    }

    @PutMapping("/boarding-houses/{id}")
    public ResponseEntity<BoardingHouse> updateBoardingHouse(
            @PathVariable UUID id,
            @RequestBody BoardingHouse boardingHouse,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.updateBoardingHouse(id, boardingHouse, landlord));
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
    public ResponseEntity<Room> createRoom(
            @PathVariable UUID bhId,
            @RequestBody Room room,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.createRoom(bhId, room, landlord));
    }

    @GetMapping("/boarding-houses/{bhId}/rooms")
    public ResponseEntity<Page<Room>> getRoomsByBoardingHouse(
            @PathVariable UUID bhId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getRoomsByBoardingHouse(bhId, landlord, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<Room>> getAllRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getRoomsByLandlord(landlord, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getRoomById(id, landlord));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable UUID id,
            @RequestBody Room room,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.updateRoom(id, room, landlord));
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
    public ResponseEntity<ExtraFee> createExtraFee(
            @PathVariable UUID bhId,
            @RequestBody ExtraFee extraFee,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.createExtraFee(bhId, extraFee, landlord));
    }

    @GetMapping("/boarding-houses/{bhId}/extra-fees")
    public ResponseEntity<List<ExtraFee>> getExtraFees(
            @PathVariable UUID bhId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(roomService.getExtraFeesByBoardingHouse(bhId, landlord));
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
