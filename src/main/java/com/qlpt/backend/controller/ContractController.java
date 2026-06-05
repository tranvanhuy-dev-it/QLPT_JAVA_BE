package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.ContractCreateRequest;
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
    public ResponseEntity<Contract> createContract(
            @Valid @RequestBody ContractCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(contractService.createContract(request, landlord));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Contract> terminateContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(contractService.terminateContract(id, landlord));
    }

    @GetMapping
    public ResponseEntity<Page<Contract>> getContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = userDetails.getUser();
        if (user.getRole() == Role.LANDLORD) {
            return ResponseEntity.ok(contractService.getContractsByLandlord(user, pageable));
        } else if (user.getRole() == Role.TENANT) {
            return ResponseEntity.ok(contractService.getContractsByTenant(user, pageable));
        } else {
            throw new RuntimeException("Tài khoản của bạn không được cấp quyền thực hiện chức năng này");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(contractService.getContractById(id, user));
    }
}
