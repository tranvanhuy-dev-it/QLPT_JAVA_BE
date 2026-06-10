package com.qlpt.backend.service;

import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraResponse;
import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.BoardingHouseCamera;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.BoardingHouseCameraRepository;
import com.qlpt.backend.repository.BoardingHouseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface BoardingHouseCameraService {
    public List<BoardingHouseCameraResponse> getCameras(UUID boardingHouseId, User landlord);
    public List<BoardingHouseCameraResponse> getAllCameras(User landlord);
    public List<BoardingHouseCameraResponse> getTenantCameras(User tenant);
    public BoardingHouseCameraResponse addCamera(UUID boardingHouseId, BoardingHouseCameraCreateRequest request, User landlord);
    public BoardingHouseCameraResponse updateCamera(UUID cameraId, BoardingHouseCameraCreateRequest request, User landlord);
    public void deleteCamera(UUID cameraId, User landlord);
}
