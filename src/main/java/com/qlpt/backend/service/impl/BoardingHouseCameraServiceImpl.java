package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.BoardingHouseCameraService;

import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraCreateRequest;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseCameraResponse;
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
public class BoardingHouseCameraServiceImpl implements BoardingHouseCameraService {

    private final BoardingHouseRepository boardingHouseRepository;
    private final BoardingHouseCameraRepository boardingHouseCameraRepository;
    private final ImouCloudService imouCloudService;

    public BoardingHouseCameraServiceImpl(BoardingHouseRepository boardingHouseRepository,
            BoardingHouseCameraRepository boardingHouseCameraRepository,
            ImouCloudService imouCloudService) {
        this.boardingHouseRepository = boardingHouseRepository;
        this.boardingHouseCameraRepository = boardingHouseCameraRepository;
        this.imouCloudService = imouCloudService;
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
    @Override
    public List<BoardingHouseCameraResponse> getCameras(UUID boardingHouseId, User landlord) {
        getBoardingHouseAndVerifyLandlord(boardingHouseId, landlord);

        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository
                .findByBoardingHouseIdOrderByCreatedAtAsc(boardingHouseId);
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<BoardingHouseCameraResponse> getAllCameras(User landlord) {
        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository
                .findByBoardingHouseLandlordIdOrderByCreatedAtAsc(landlord.getId());
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<BoardingHouseCameraResponse> getTenantCameras(User tenant) {
        List<BoardingHouseCamera> cameras = boardingHouseCameraRepository.findTenantCameras(tenant.getId());
        return cameras.stream()
                .map(BoardingHouseCameraResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public BoardingHouseCameraResponse addCamera(UUID boardingHouseId, BoardingHouseCameraCreateRequest request,
            User landlord) {
        BoardingHouse house = getBoardingHouseAndVerifyLandlord(boardingHouseId, landlord);

        String brand = request.brand() != null ? request.brand().toUpperCase() : "CUSTOM";
        String streamUrl = request.streamUrl();

        if ("IMOU".equals(brand)) {
            if (request.serialNumber() == null || request.serialNumber().trim().isEmpty() ||
                request.safetyCode() == null || request.safetyCode().trim().isEmpty()) {
                throw new RuntimeException("Số Serial (S/N) và Mã an toàn không được để trống đối với Camera Imou");
            }
            // 1. Liên kết thiết bị lên tài khoản Cloud Developer trước
            imouCloudService.bindDevice(request.serialNumber().trim(), request.safetyCode().trim());
            // 2. Lấy URL luồng phát trực tiếp
            streamUrl = imouCloudService.getLiveStreamUrl(request.serialNumber().trim(), request.safetyCode().trim());
        } else {
            if (streamUrl == null || streamUrl.trim().isEmpty()) {
                throw new RuntimeException("Đường dẫn luồng (stream URL) không được để trống");
            }
        }

        BoardingHouseCamera camera = BoardingHouseCamera.builder()
                .name(request.name())
                .streamUrl(streamUrl)
                .username("IMOU".equals(brand) ? null : request.username())
                .password("IMOU".equals(brand) ? null : request.password())
                .brand(brand)
                .serialNumber(request.serialNumber())
                .safetyCode(request.safetyCode())
                .boardingHouse(house)
                .createdAt(LocalDateTime.now())
                .build();

        BoardingHouseCamera saved = boardingHouseCameraRepository.save(camera);
        return BoardingHouseCameraResponse.fromEntity(saved);
    }

    @Transactional
    @Override
    public BoardingHouseCameraResponse updateCamera(UUID cameraId, BoardingHouseCameraCreateRequest request,
            User landlord) {
        BoardingHouseCamera camera = boardingHouseCameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera"));

        getBoardingHouseAndVerifyLandlord(camera.getBoardingHouse().getId(), landlord);

        String brand = request.brand() != null ? request.brand().toUpperCase() : "CUSTOM";
        String streamUrl = request.streamUrl();

        if ("IMOU".equals(brand)) {
            if (request.serialNumber() == null || request.serialNumber().trim().isEmpty() ||
                request.safetyCode() == null || request.safetyCode().trim().isEmpty()) {
                throw new RuntimeException("Số Serial (S/N) và Mã an toàn không được để trống đối với Camera Imou");
            }
            // Chỉ liên kết lại và lấy lại stream URL mới nếu S/N hoặc Mã an toàn thay đổi, hoặc nếu stream URL hiện tại trống
            if (camera.getStreamUrl() == null || !brand.equals(camera.getBrand()) ||
                !request.serialNumber().equals(camera.getSerialNumber()) ||
                !request.safetyCode().equals(camera.getSafetyCode())) {
                imouCloudService.bindDevice(request.serialNumber().trim(), request.safetyCode().trim());
                streamUrl = imouCloudService.getLiveStreamUrl(request.serialNumber().trim(), request.safetyCode().trim());
            } else {
                streamUrl = camera.getStreamUrl();
            }
        } else {
            if (streamUrl == null || streamUrl.trim().isEmpty()) {
                throw new RuntimeException("Đường dẫn luồng (stream URL) không được để trống");
            }
        }

        camera.setName(request.name());
        camera.setStreamUrl(streamUrl);
        camera.setUsername("IMOU".equals(brand) ? null : request.username());
        camera.setPassword("IMOU".equals(brand) ? null : request.password());
        camera.setBrand(brand);
        camera.setSerialNumber(request.serialNumber());
        camera.setSafetyCode(request.safetyCode());

        BoardingHouseCamera updated = boardingHouseCameraRepository.save(camera);
        return BoardingHouseCameraResponse.fromEntity(updated);
    }

    @Transactional
    @Override
    public void deleteCamera(UUID cameraId, User landlord) {
        BoardingHouseCamera camera = boardingHouseCameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera"));

        getBoardingHouseAndVerifyLandlord(camera.getBoardingHouse().getId(), landlord);

        boardingHouseCameraRepository.delete(camera);
    }

    @Transactional(readOnly = true)
    @Override
    public String getCameraStreamUrl(UUID cameraId, User user) {
        BoardingHouseCamera camera = boardingHouseCameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera"));

        // Phân quyền
        if ("LANDLORD".equals(user.getRole().name())) {
            if (!camera.getBoardingHouse().getLandlord().getId().equals(user.getId())) {
                throw new RuntimeException("Bạn không có quyền truy cập camera này");
            }
        } else if ("TENANT".equals(user.getRole().name())) {
            List<BoardingHouseCamera> tenantCameras = boardingHouseCameraRepository.findTenantCameras(user.getId());
            boolean hasAccess = tenantCameras.stream().anyMatch(c -> c.getId().equals(cameraId));
            if (!hasAccess) {
                throw new RuntimeException("Bạn không có quyền truy cập camera này");
            }
        } else {
            throw new RuntimeException("Vai trò không hợp lệ");
        }

        // Nếu là IMOU, lấy URL luồng trực tiếp động mới từ Cloud
        if ("IMOU".equals(camera.getBrand())) {
            return imouCloudService.getLiveStreamUrl(camera.getSerialNumber().trim(), camera.getSafetyCode().trim());
        }

        // Nếu là CUSTOM, trả về URL đã lưu tĩnh
        return camera.getStreamUrl();
    }
}
