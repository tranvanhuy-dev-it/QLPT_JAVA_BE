package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.InvoiceCreateRequest;
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

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Invoice> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(invoiceService.createInvoice(request, landlord));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Invoice> payInvoice(
            @PathVariable UUID id,
            @RequestBody Map<String, Double> payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        double paidAmount = payload.getOrDefault("paidAmount", 0.0);
        return ResponseEntity.ok(invoiceService.updatePaymentStatus(id, paidAmount, landlord));
    }

    @GetMapping
    public ResponseEntity<Page<Invoice>> getInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = userDetails.getUser();
        if (user.getRole() == Role.LANDLORD) {
            return ResponseEntity.ok(invoiceService.getInvoicesByLandlord(user, pageable));
        } else if (user.getRole() == Role.TENANT) {
            return ResponseEntity.ok(invoiceService.getInvoicesByTenant(user, pageable));
        } else {
            throw new RuntimeException("Tài khoản của bạn không được cấp quyền thực hiện chức năng này");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, user));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<InvoiceItem>> getInvoiceItems(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(invoiceService.getInvoiceItems(id, user));
    }
}
