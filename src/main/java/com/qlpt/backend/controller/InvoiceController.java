package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.InvoiceCreateRequest;
import com.qlpt.backend.dto.InvoiceItemResponse;
import com.qlpt.backend.dto.InvoiceResponse;
import com.qlpt.backend.entity.Invoice;
import com.qlpt.backend.entity.InvoiceItem;
import com.qlpt.backend.entity.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Invoice created = invoiceService.createInvoice(request, landlord);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(created));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable UUID id,
            @RequestBody Map<String, Double> payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        double paidAmount = payload.getOrDefault("paidAmount", 0.0);
        Invoice updated = invoiceService.updatePaymentStatus(id, paidAmount, landlord);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(updated));
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = userDetails.getUser();
        Page<Invoice> invoices;
        if (user.getRole() == Role.LANDLORD) {
            invoices = invoiceService.getInvoicesByLandlord(user, pageable);
        } else if (user.getRole() == Role.TENANT) {
            invoices = invoiceService.getInvoicesByTenant(user, pageable);
        } else {
            throw new RuntimeException("Tài khoản của bạn không được cấp quyền thực hiện chức năng này");
        }
        return ResponseEntity.ok(invoices.map(InvoiceResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        Invoice invoice = invoiceService.getInvoiceById(id, user);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<InvoiceItemResponse>> getInvoiceItems(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<InvoiceItem> items = invoiceService.getInvoiceItems(id, user);
        List<InvoiceItemResponse> dtos = items.stream()
                .map(InvoiceItemResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
