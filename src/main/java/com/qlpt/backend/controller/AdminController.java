package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.UserResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.UserService;
import com.qlpt.backend.repository.UserRepository;
import com.qlpt.backend.repository.BoardingHouseRepository;
import com.qlpt.backend.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BoardingHouseRepository boardingHouseRepository;
    private final RoomRepository roomRepository;

    public AdminController(UserService userService,
                           UserRepository userRepository,
                           BoardingHouseRepository boardingHouseRepository,
                           RoomRepository roomRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.boardingHouseRepository = boardingHouseRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/landlords")
    public ResponseEntity<Page<UserResponse>> getAllLandlords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User admin = userDetails.getUser();
        Page<User> landlords = userService.getAllLandlords(admin, pageable);
        return ResponseEntity.ok(landlords.map(UserResponse::fromEntity));
    }

    @PostMapping("/landlords/{id}/toggle")
    public ResponseEntity<UserResponse> toggleLandlordStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User admin = userDetails.getUser();
        User toggled = userService.toggleUserStatus(id, admin);
        return ResponseEntity.ok(UserResponse.fromEntity(toggled));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        long totalLandlords = userRepository.countByRole(com.qlpt.backend.entity.Role.LANDLORD);
        long totalTenants = userRepository.countByRole(com.qlpt.backend.entity.Role.TENANT);
        long totalUsers = userRepository.count();
        
        long totalBoardingHouses = boardingHouseRepository.count();
        long totalRooms = roomRepository.count();
        long occupiedRooms = roomRepository.countByStatus(com.qlpt.backend.entity.RoomStatus.OCCUPIED);
        
        double occupancyRate = 0.0;
        if (totalRooms > 0) {
            occupancyRate = (occupiedRooms * 100.0) / totalRooms;
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLandlords", totalLandlords);
        stats.put("totalTenants", totalTenants);
        stats.put("totalUsers", totalUsers);
        stats.put("totalBoardingHouses", totalBoardingHouses);
        stats.put("totalRooms", totalRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);
        
        return ResponseEntity.ok(stats);
    }
}
