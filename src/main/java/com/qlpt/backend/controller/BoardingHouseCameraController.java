package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.BoardingHouseCameraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/boarding-houses")
@PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
public class BoardingHouseCameraController {

    private final BoardingHouseCameraService cameraService;

    public BoardingHouseCameraController(BoardingHouseCameraService cameraService) {
        this.cameraService = cameraService;
    }

    @GetMapping("/tenant/cameras")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<BoardingHouseCameraResponse>> getTenantCameras(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User tenant = userDetails.getUser();
        List<BoardingHouseCameraResponse> cameras = cameraService.getTenantCameras(tenant);
        return ResponseEntity.ok(cameras);
    }

    @GetMapping("/cameras")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<BoardingHouseCameraResponse>> getAllCameras(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<BoardingHouseCameraResponse> cameras = cameraService.getAllCameras(landlord);
        return ResponseEntity.ok(cameras);
    }

    @GetMapping("/{houseId}/cameras")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<BoardingHouseCameraResponse>> getCameras(
            @PathVariable UUID houseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<BoardingHouseCameraResponse> cameras = cameraService.getCameras(houseId, landlord);
        return ResponseEntity.ok(cameras);
    }

    @PostMapping("/{houseId}/cameras")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<BoardingHouseCameraResponse> addCamera(
            @PathVariable UUID houseId,
            @Valid @RequestBody BoardingHouseCameraCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouseCameraResponse camera = cameraService.addCamera(houseId, request, landlord);
        return ResponseEntity.ok(camera);
    }

    @PutMapping("/cameras/{cameraId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<BoardingHouseCameraResponse> updateCamera(
            @PathVariable UUID cameraId,
            @Valid @RequestBody BoardingHouseCameraCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        BoardingHouseCameraResponse camera = cameraService.updateCamera(cameraId, request, landlord);
        return ResponseEntity.ok(camera);
    }

    @DeleteMapping("/cameras/{cameraId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Void> deleteCamera(
            @PathVariable UUID cameraId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        cameraService.deleteCamera(cameraId, landlord);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cameras/{cameraId}/stream")
    public ResponseEntity<Map<String, String>> getCameraStream(
            @PathVariable UUID cameraId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        String streamUrl = cameraService.getCameraStreamUrl(cameraId, user);
        return ResponseEntity.ok(Map.of("streamUrl", streamUrl));
    }
}
