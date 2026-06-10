package com.qlpt.backend.service;

import com.qlpt.backend.dto.contract.ContractAddendumCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContractAddendumService {

    private final ContractRepository contractRepository;
    private final ContractAddendumRepository contractAddendumRepository;
    private final ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository;
    private final ExtraFeeRepository extraFeeRepository;

    public ContractAddendumService(ContractRepository contractRepository,
                                    ContractAddendumRepository contractAddendumRepository,
                                    ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository,
                                    ExtraFeeRepository extraFeeRepository) {
        this.contractRepository = contractRepository;
        this.contractAddendumRepository = contractAddendumRepository;
        this.contractAddendumExtraFeeRepository = contractAddendumExtraFeeRepository;
        this.extraFeeRepository = extraFeeRepository;
    }

    @Transactional
    public ContractAddendum createAddendum(UUID contractId, ContractAddendumCreateRequest request, User landlord) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        // Kiểm tra quyền chủ trọ
        if (!contract.getRoom().getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền thêm phụ lục cho hợp đồng này");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Hợp đồng không còn hoạt động, không thể thêm phụ lục");
        }

        // Kiểm tra ngày hiệu lực không trước ngày bắt đầu hợp đồng
        if (request.getStartDate().isBefore(contract.getStartDate())) {
            throw new RuntimeException("Ngày hiệu lực phụ lục không được trước ngày bắt đầu hợp đồng");
        }

        // Kiểm tra số người ở
        if (request.getNumberOfTenants() > contract.getRoom().getMaxPeople()) {
            throw new RuntimeException("Số người ở vượt quá sức chứa tối đa của phòng (" + contract.getRoom().getMaxPeople() + " người)");
        }

        ContractAddendum addendum = ContractAddendum.builder()
                .contract(contract)
                .startDate(request.getStartDate())
                .roomPrice(request.getRoomPrice())
                .electricityRate(request.getElectricityRate())
                .waterRate(request.getWaterRate())
                .waterBillingType(request.getWaterBillingType())
                .numberOfTenants(request.getNumberOfTenants())
                .description(request.getDescription())
                .build();

        ContractAddendum savedAddendum = contractAddendumRepository.save(addendum);

        // Lưu các phụ phí tùy chỉnh cho phụ lục
        if (request.getExtraFees() != null) {
            for (ContractAddendumCreateRequest.ExtraFeeOverride override : request.getExtraFees()) {
                ExtraFee extraFee = extraFeeRepository.findById(override.getExtraFeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ phụ phí: " + override.getExtraFeeId()));

                double feePrice = override.getCustomPrice() > 0 ? override.getCustomPrice() : extraFee.getDefaultPrice();

                ContractAddendumExtraFee addendumExtraFee = ContractAddendumExtraFee.builder()
                        .addendum(savedAddendum)
                        .extraFee(extraFee)
                        .customPrice(feePrice)
                        .build();

                contractAddendumExtraFeeRepository.save(addendumExtraFee);
            }
        }

        // Cập nhật số người ở trên Contract cho đồng bộ
        contract.setNumberOfTenants(request.getNumberOfTenants());
        contractRepository.save(contract);

        return savedAddendum;
    }

    @Transactional(readOnly = true)
    public List<ContractAddendum> getAddendumsByContract(UUID contractId, User user) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        boolean isLandlord = contract.getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = contract.getTenant().getId().equals(user.getId());

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem phụ lục của hợp đồng này");
        }

        return contractAddendumRepository.findByContractIdOrderByStartDateDesc(contractId);
    }

    @Transactional(readOnly = true)
    public List<ContractAddendumExtraFee> getAddendumExtraFees(UUID addendumId, User user) {
        ContractAddendum addendum = contractAddendumRepository.findById(addendumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ lục hợp đồng"));

        Contract contract = addendum.getContract();
        boolean isLandlord = contract.getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = contract.getTenant().getId().equals(user.getId());

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem phụ phí của phụ lục này");
        }

        return contractAddendumExtraFeeRepository.findByAddendumId(addendumId);
    }
}
