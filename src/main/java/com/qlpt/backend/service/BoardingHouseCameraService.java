package com.qlpt.backend.service;

import com.qlpt.backend.dto.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.BoardingHouseCameraResponse;
import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.BoardingHouseCamera;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.BoardingHouseCameraRepository;
import com.qlpt.backend.repository.BoardingHouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardingHouseCameraService {

    private final BoardingHouseRepository boardingHouseRepository;
    private final BoardingHouseCameraRepository boardingHouseCameraRepository;

    public BoardingHouseCameraService(BoardingHouseRepository boardingHouseRepository,
                                      BoardingHouseCameraRepository boardingHouseCameraRepository) {
        this.boardingHouseRepository = boardingHouseRepository;
        this.boardingHouseCameraRepository = boardingHouseCameraRepository;
    }

    private BoardingHouse getBoardingHouseAndVerifyLandlord(UUID boardingHouseId, User landlord) {
        BoardingHouse house = boardingHouseRepository.findById(boardingHouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dãy trọ"));
        
        if (!house.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý dãy trọ này");
        }
        return house;
    }

    @Transactional(readOnly = true)
    public List<BoardingHouseCameraResponse> getCameras(UUID boardingHouseId, User landlord) {
        // Validate landlord has access to the boarding house
        getBoardingHouseAndVerifyLandlord(boardingHouseId, landlord);
        
        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository.findByBoardingHouseIdOrderByCreatedAtAsc(boardingHouseId);
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BoardingHouseCameraResponse> getAllCameras(User landlord) {
        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository.findByBoardingHouseLandlordIdOrderByCreatedAtAsc(landlord.getId());
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BoardingHouseCameraResponse> getTenantCameras(User tenant) {
        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository.findTenantCameras(tenant.getId());
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BoardingHouseCameraResponse addCamera(UUID boardingHouseId, BoardingHouseCameraCreateRequest request, User landlord) {
        BoardingHouse house = getBoardingHouseAndVerifyLandlord(boardingHouseId, landlord);

        BoardingHouseCamera camera = BoardingHouseCamera.builder()
                .name(request.name())
                .streamUrl(request.streamUrl())
                .username(request.username())
                .password(request.password())
                .boardingHouse(house)
                .createdAt(LocalDateTime.now())
                .build();

        BoardingHouseCamera saved = boardingHouseCameraRepository.save(camera);
        return BoardingHouseCameraResponse.fromEntity(saved);
    }

    @Transactional
    public BoardingHouseCameraResponse updateCamera(UUID cameraId, BoardingHouseCameraCreateRequest request, User landlord) {
        BoardingHouseCamera camera = boardingHouseCameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera"));

        // Verify ownership
        getBoardingHouseAndVerifyLandlord(camera.getBoardingHouse().getId(), landlord);

        camera.setName(request.name());
        camera.setStreamUrl(request.streamUrl());
        camera.setUsername(request.username());
        camera.setPassword(request.password());

        BoardingHouseCamera updated = boardingHouseCameraRepository.save(camera);
        return BoardingHouseCameraResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCamera(UUID cameraId, User landlord) {
        BoardingHouseCamera camera = boardingHouseCameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera"));

        // Verify ownership
        getBoardingHouseAndVerifyLandlord(camera.getBoardingHouse().getId(), landlord);

        boardingHouseCameraRepository.delete(camera);
    }
}
