package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.ContractService;
import com.qlpt.backend.service.NotificationService;

import com.qlpt.backend.dto.contract.ContractCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ExtraFeeRepository extraFeeRepository;
    private final ContractExtraFeeRepository contractExtraFeeRepository;
    private final ContractAddendumRepository contractAddendumRepository;
    private final ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository;
    private final NotificationService notificationService;

    public ContractServiceImpl(ContractRepository contractRepository,
                           RoomRepository roomRepository,
                           UserRepository userRepository,
                           ExtraFeeRepository extraFeeRepository,
                           ContractExtraFeeRepository contractExtraFeeRepository,
                           ContractAddendumRepository contractAddendumRepository,
                           ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository,
                           NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.extraFeeRepository = extraFeeRepository;
        this.contractExtraFeeRepository = contractExtraFeeRepository;
        this.contractAddendumRepository = contractAddendumRepository;
        this.contractAddendumExtraFeeRepository = contractAddendumExtraFeeRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    @Override
    public Contract createContract(ContractCreateRequest request, User landlord) {
        // Find and validate room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng trọ"));

        if (!room.getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý phòng trọ này");
        }

        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new RuntimeException("Phòng trọ này hiện đang được thuê, không thể tạo hợp đồng mới");
        }

        // Find and validate tenant
        User tenant = userRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người thuê"));

        if (tenant.getRole() != Role.TENANT) {
            throw new RuntimeException("Tài khoản được chọn không phải là vai trò Người Thuê");
        }

        double finalRoomPrice = request.getContractedRoomPrice() > 0 ? request.getContractedRoomPrice() : room.getBasePrice();

        Integer fixedBillingDay = request.getFixedBillingDay() != null ? request.getFixedBillingDay() : room.getBoardingHouse().getFixedBillingDay();

        // Create contract
        Contract contract = Contract.builder()
                .room(room)
                .tenant(tenant)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deposit(request.getDeposit())
                .contractedRoomPrice(finalRoomPrice)
                .numberOfTenants(request.getNumberOfTenants())
                .fixedBillingDay(fixedBillingDay)
                .status(ContractStatus.ACTIVE)
                .build();

        Contract savedContract = contractRepository.save(contract);

        // Create initial ContractAddendum
        ContractAddendum initialAddendum = ContractAddendum.builder()
                .contract(savedContract)
                .startDate(request.getStartDate())
                .roomPrice(finalRoomPrice)
                .electricityRate(room.getBoardingHouse().getDefaultElectricityRate())
                .waterRate(room.getBoardingHouse().getDefaultWaterRate())
                .waterBillingType(room.getBoardingHouse().getWaterBillingType())
                .numberOfTenants(request.getNumberOfTenants())
                .description("Phụ lục gốc")
                .build();

        ContractAddendum savedAddendum = contractAddendumRepository.save(initialAddendum);

        // Update room status
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        // Save extra fees associated with the contract
        if (request.getExtraFees() != null) {
            for (ContractCreateRequest.ExtraFeeOverride override : request.getExtraFees()) {
                ExtraFee extraFee = extraFeeRepository.findById(override.getExtraFeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ phụ phí: " + override.getExtraFeeId()));

                double feePrice = override.getCustomPrice() > 0 ? override.getCustomPrice() : extraFee.getDefaultPrice();

                ContractExtraFee contractExtraFee = ContractExtraFee.builder()
                        .contract(savedContract)
                        .extraFee(extraFee)
                        .customPrice(feePrice)
                        .build();

                contractExtraFeeRepository.save(contractExtraFee);

                ContractAddendumExtraFee addendumExtraFee = ContractAddendumExtraFee.builder()
                        .addendum(savedAddendum)
                        .extraFee(extraFee)
                        .customPrice(feePrice)
                        .build();

                contractAddendumExtraFeeRepository.save(addendumExtraFee);
            }
        }

        Contract resultContract = contractRepository.findWithDetailsById(savedContract.getId()).orElse(savedContract);

        // Gửi thông báo đến người thuê
        try {
            String title = "Hợp đồng thuê phòng hoạt động";
            String content = String.format("Hợp đồng thuê phòng %s tại nhà trọ %s của bạn đã được kích hoạt thành công.",
                    room.getRoomNumber(),
                    room.getBoardingHouse().getName());
            notificationService.createNotification(tenant, title, content, "CONTRACT_ACTIVE");
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi thông báo hợp đồng hoạt động: " + e.getMessage());
        }

        return resultContract;
    }

    @Transactional
    @Override
    public Contract terminateContract(UUID contractId, User landlord) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        if (!contract.getRoom().getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền chấm dứt hợp đồng này");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Hợp đồng này không còn hoạt động");
        }

        // Mark contract as terminated
        contract.setStatus(ContractStatus.TERMINATED);
        contractRepository.save(contract);

        // Make room vacant
        Room room = contract.getRoom();
        room.setStatus(RoomStatus.VACANT);
        roomRepository.save(room);

        // Lock tenant's account
        User tenant = contract.getTenant();
        tenant.setStatus("INACTIVE");
        userRepository.save(tenant);

        return contractRepository.findWithDetailsById(contract.getId()).orElse(contract);
    }

    @Transactional
    @Override
    public Contract updateContract(UUID id, int numberOfTenants, User landlord) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        if (!contract.getRoom().getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa hợp đồng này");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Hợp đồng không còn hoạt động, không thể chỉnh sửa");
        }

        if (numberOfTenants < 1) {
            throw new RuntimeException("Số người ở phải lớn hơn hoặc bằng 1");
        }

        if (numberOfTenants > contract.getRoom().getMaxPeople()) {
            throw new RuntimeException("Số người ở vượt quá sức chứa tối đa của phòng (" + contract.getRoom().getMaxPeople() + " người)");
        }

        Contract saved = contractRepository.save(contract);
        return contractRepository.findWithDetailsById(saved.getId()).orElse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Contract> getContractsByLandlord(User landlord, Pageable pageable) {
        return contractRepository.findByRoomBoardingHouseLandlordId(landlord.getId(), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Contract> getContractsByRoomAndLandlord(UUID roomId, User landlord, Pageable pageable) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng trọ"));
        if (!room.getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý phòng trọ này");
        }
        return contractRepository.findByRoomId(roomId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Contract getContractById(UUID contractId, User user) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        System.out.println("=== DEBUGLOG: getContractById ===");
        System.out.println("Logged in user ID: " + user.getId() + " (" + user.getUsername() + ", Role: " + user.getRole() + ")");
        if (contract.getRoom() != null && contract.getRoom().getBoardingHouse() != null && contract.getRoom().getBoardingHouse().getLandlord() != null) {
            System.out.println("Contract Landlord ID: " + contract.getRoom().getBoardingHouse().getLandlord().getId() + " (" + contract.getRoom().getBoardingHouse().getLandlord().getUsername() + ")");
        } else {
            System.out.println("Contract room/boarding house/landlord is NULL!");
        }
        if (contract.getTenant() != null) {
            System.out.println("Contract Tenant ID: " + contract.getTenant().getId() + " (" + contract.getTenant().getUsername() + ")");
        } else {
            System.out.println("Contract Tenant is NULL!");
        }

        // Allow landlord or tenant of this contract to view it
        boolean isLandlord = contract.getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = contract.getTenant().getId().equals(user.getId());

        System.out.println("isLandlord: " + isLandlord + ", isTenant: " + isTenant);

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem thông tin hợp đồng này");
        }

        return contract;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Contract> getContractsByTenant(User tenant, Pageable pageable) {
        return contractRepository.findByTenantId(tenant.getId(), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContractExtraFee> getContractExtraFees(UUID contractId, User user) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        
        boolean isLandlord = contract.getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        
        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem thông tin phụ phí của hợp đồng này");
        }
        
        return contractExtraFeeRepository.findByContractId(contractId);
    }
}
