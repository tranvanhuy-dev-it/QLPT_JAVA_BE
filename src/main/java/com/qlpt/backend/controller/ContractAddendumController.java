package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.ContractAddendumCreateRequest;
import com.qlpt.backend.dto.ContractAddendumExtraFeeResponse;
import com.qlpt.backend.dto.ContractAddendumResponse;
import com.qlpt.backend.entity.ContractAddendum;
import com.qlpt.backend.entity.ContractAddendumExtraFee;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.ContractAddendumService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts/{contractId}/addendums")
public class ContractAddendumController {

    private final ContractAddendumService contractAddendumService;

    public ContractAddendumController(ContractAddendumService contractAddendumService) {
        this.contractAddendumService = contractAddendumService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractAddendumResponse> createAddendum(
            @PathVariable UUID contractId,
            @Valid @RequestBody ContractAddendumCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        ContractAddendum addendum = contractAddendumService.createAddendum(contractId, request, landlord);
        return ResponseEntity.ok(ContractAddendumResponse.fromEntity(addendum));
    }

    @GetMapping
    public ResponseEntity<List<ContractAddendumResponse>> getAddendums(
            @PathVariable UUID contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<ContractAddendum> addendums = contractAddendumService.getAddendumsByContract(contractId, user);
        List<ContractAddendumResponse> response = addendums.stream()
                .map(ContractAddendumResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{addendumId}/extra-fees")
    public ResponseEntity<List<ContractAddendumExtraFeeResponse>> getAddendumExtraFees(
            @PathVariable UUID contractId,
            @PathVariable UUID addendumId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<ContractAddendumExtraFee> fees = contractAddendumService.getAddendumExtraFees(addendumId, user);
        List<ContractAddendumExtraFeeResponse> response = fees.stream()
                .map(ContractAddendumExtraFeeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
