package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.BoardingHouseCameraResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.BoardingHouseCameraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/boarding-houses")
@PreAuthorize("hasRole('LANDLORD')")
public class BoardingHouseCameraController {

    private final BoardingHouseCameraService cameraService;

    public BoardingHouseCameraController(BoardingHouseCameraService cameraService) {
        this.cameraService = cameraService;
    }

    @GetMapping("/cameras")
    public ResponseEntity<List<BoardingHouseCameraResponse>> getAllCameras(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<BoardingHouseCameraResponse> cameras = cameraService.getAllCameras(landlord);
        return ResponseEntity.ok(cameras);
    }

    @GetMapping("/{houseId}/cameras")
    public ResponseEntity<List<BoardingHouseCameraResponse>> getCameras(
            @PathVariable UUID houseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<BoardingHouseCameraResponse> cameras = cameraService.getCameras(houseId, landlord);
        return ResponseEntity.ok(cameras);
    }

    @PostMapping("/{houseId}/cameras")
    public ResponseEntity<BoardingHouseCameraResponse> addCamera(
            @PathVariable UUID houseId,
            @Valid @RequestBody BoardingHouseCameraCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouseCameraResponse camera = cameraService.addCamera(houseId, request, landlord);
        return ResponseEntity.ok(camera);
    }

    @PutMapping("/cameras/{cameraId}")
    public ResponseEntity<BoardingHouseCameraResponse> updateCamera(
            @PathVariable UUID cameraId,
            @Valid @RequestBody BoardingHouseCameraCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouseCameraResponse camera = cameraService.updateCamera(cameraId, request, landlord);
        return ResponseEntity.ok(camera);
    }

    @DeleteMapping("/cameras/{cameraId}")
    public ResponseEntity<Void> deleteCamera(
            @PathVariable UUID cameraId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        cameraService.deleteCamera(cameraId, landlord);
        return ResponseEntity.ok().build();
    }
}
