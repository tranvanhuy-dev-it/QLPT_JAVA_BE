package com.qlpt.backend.dto;

import com.qlpt.backend.entity.Invoice;
import com.qlpt.backend.entity.InvoiceStatus;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    LocalDate invoiceDate,
    LocalDate billingPeriodStart,
    LocalDate billingPeriodEnd,
    double oldElectricityIndex,
    double newElectricityIndex,
    double electricityRate,
    double oldWaterIndex,
    double newWaterIndex,
    double waterRate,
    double roomPrice,
    double totalAmount,
    double paidAmount,
    InvoiceStatus status,
    LocalDate paymentDate,
    ContractResponse contract
) {
    public static InvoiceResponse fromEntity(Invoice invoice) {
        if (invoice == null) return null;
        try {
            invoice.getInvoiceDate();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceDate(),
            invoice.getBillingPeriodStart(),
            invoice.getBillingPeriodEnd(),
            invoice.getOldElectricityIndex(),
            invoice.getNewElectricityIndex(),
            invoice.getElectricityRate(),
            invoice.getOldWaterIndex(),
            invoice.getNewWaterIndex(),
            invoice.getWaterRate(),
            invoice.getRoomPrice(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            invoice.getStatus(),
            invoice.getPaymentDate(),
            ContractResponse.fromEntity(invoice.getContract())
        );
    }
}
