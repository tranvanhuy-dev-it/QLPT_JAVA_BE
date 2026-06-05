package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.ContractCreateRequest;
import com.qlpt.backend.dto.ContractResponse;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.Role;
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

import java.util.UUID;

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

    @GetMapping
    public ResponseEntity<Page<ContractResponse>> getContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = userDetails.getUser();
        Page<Contract> contracts;
        if (user.getRole() == Role.LANDLORD) {
            contracts = contractService.getContractsByLandlord(user, pageable);
        } else if (user.getRole() == Role.TENANT) {
            contracts = contractService.getContractsByTenant(user, pageable);
        } else {
            throw new RuntimeException("Tài khoản của bạn không được cấp quyền thực hiện chức năng này");
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
}
