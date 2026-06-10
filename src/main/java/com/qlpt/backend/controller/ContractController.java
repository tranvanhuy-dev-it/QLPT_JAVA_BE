package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.contract.ContractCreateRequest;
import com.qlpt.backend.dto.contract.ContractResponse;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.qlpt.backend.dto.contract.ContractExtraFeeResponse;
import com.qlpt.backend.entity.ContractExtraFee;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody ContractCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Contract created = contractService.createContract(request, landlord);
        return ResponseEntity.ok(ContractResponse.fromEntity(created));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> terminateContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Contract terminated = contractService.terminateContract(id, landlord);
        return ResponseEntity.ok(ContractResponse.fromEntity(terminated));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> updateContract(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        int numberOfTenants = ((Number) body.get("numberOfTenants")).intValue();
        Contract updated = contractService.updateContract(id, numberOfTenants, landlord);
        return ResponseEntity.ok(ContractResponse.fromEntity(updated));
    }

    @GetMapping
    public ResponseEntity<Page<ContractResponse>> getContracts(
            @RequestParam(required = false) UUID roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "startDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        User user = userDetails.getUser();
        Page<Contract> contracts;
        if (roomId != null) {
            if (user.getRole() == Role.LANDLORD) {
                contracts = contractService.getContractsByRoomAndLandlord(roomId, user, pageable);
            } else {
                throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
            }
        } else {
            if (user.getRole() == Role.LANDLORD) {
                contracts = contractService.getContractsByLandlord(user, pageable);
            } else if (user.getRole() == Role.TENANT) {
                contracts = contractService.getContractsByTenant(user, pageable);
            } else {
                throw new RuntimeException("Tài khoản của bạn không được cấp quyền thực hiện chức năng này");
            }
        }
        return ResponseEntity.ok(contracts.map(ContractResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        Contract contract = contractService.getContractById(id, user);
        return ResponseEntity.ok(ContractResponse.fromEntity(contract));
    }

    @GetMapping("/{id}/extra-fees")
    public ResponseEntity<List<ContractExtraFeeResponse>> getContractExtraFees(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<ContractExtraFee> fees = contractService.getContractExtraFees(id, user);
        List<ContractExtraFeeResponse> response = fees.stream()
                .map(ContractExtraFeeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
