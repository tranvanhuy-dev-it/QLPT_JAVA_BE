package com.qlpt.backend.service;

import com.qlpt.backend.dto.ContractCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ExtraFeeRepository extraFeeRepository;
    private final ContractExtraFeeRepository contractExtraFeeRepository;

    public ContractService(ContractRepository contractRepository,
                           RoomRepository roomRepository,
                           UserRepository userRepository,
                           ExtraFeeRepository extraFeeRepository,
                           ContractExtraFeeRepository contractExtraFeeRepository) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.extraFeeRepository = extraFeeRepository;
        this.contractExtraFeeRepository = contractExtraFeeRepository;
    }

    @Transactional
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

        // Validate billing mode config
        if (request.getBillingMode() == BillingMode.FIXED_DATE_OF_MONTH) {
            if (request.getFixedBillingDay() == null || request.getFixedBillingDay() < 1 || request.getFixedBillingDay() > 31) {
                throw new RuntimeException("Chế độ ngày thanh toán cố định yêu cầu chọn ngày từ 1 đến 31");
            }
        }

        // Create contract
        Contract contract = Contract.builder()
                .room(room)
                .tenant(tenant)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deposit(request.getDeposit())
                .contractedRoomPrice(request.getContractedRoomPrice())
                .billingMode(request.getBillingMode())
                .fixedBillingDay(request.getFixedBillingDay())
                .numberOfTenants(request.getNumberOfTenants())
                .status(ContractStatus.ACTIVE)
                .build();

        Contract savedContract = contractRepository.save(contract);

        // Update room status
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        // Save extra fees associated with the contract
        if (request.getExtraFees() != null) {
            for (ContractCreateRequest.ExtraFeeOverride override : request.getExtraFees()) {
                ExtraFee extraFee = extraFeeRepository.findById(override.getExtraFeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ phụ phí: " + override.getExtraFeeId()));

                ContractExtraFee contractExtraFee = ContractExtraFee.builder()
                        .contract(savedContract)
                        .extraFee(extraFee)
                        .customPrice(override.getCustomPrice())
                        .build();

                contractExtraFeeRepository.save(contractExtraFee);
            }
        }

        return savedContract;
    }

    @Transactional
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

        return contract;
    }

    public Page<Contract> getContractsByLandlord(User landlord, Pageable pageable) {
        return contractRepository.findByRoomBoardingHouseLandlordId(landlord.getId(), pageable);
    }

    public Contract getContractById(UUID contractId, User user) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        // Allow landlord or tenant of this contract to view it
        boolean isLandlord = contract.getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = contract.getTenant().getId().equals(user.getId());

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem thông tin hợp đồng này");
        }

        return contract;
    }

    public Page<Contract> getContractsByTenant(User tenant, Pageable pageable) {
        return contractRepository.findByTenantId(tenant.getId(), pageable);
    }
}
