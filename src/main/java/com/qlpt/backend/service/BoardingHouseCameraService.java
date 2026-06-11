package com.qlpt.backend.service;

import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraResponse;
import com.qlpt.backend.entity.User;
import java.util.List;
import java.util.UUID;

public interface BoardingHouseCameraService {
    public List<BoardingHouseCameraResponse> getCameras(UUID boardingHouseId, User landlord);

    public List<BoardingHouseCameraResponse> getAllCameras(User landlord);

    public List<BoardingHouseCameraResponse> getTenantCameras(User tenant);

    public BoardingHouseCameraResponse addCamera(UUID boardingHouseId, BoardingHouseCameraCreateRequest request,
            User landlord);

    public BoardingHouseCameraResponse updateCamera(UUID cameraId, BoardingHouseCameraCreateRequest request,
            User landlord);

    public void deleteCamera(UUID cameraId, User landlord);

    public String getCameraStreamUrl(UUID cameraId, User user);
}
