package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_contract_id", columnList = "contract_id"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_invoice_date", columnList = "invoice_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "old_electricity_index", nullable = false)
    private double oldElectricityIndex;

    @Column(name = "new_electricity_index", nullable = false)
    private double newElectricityIndex;

    @Column(name = "electricity_rate", nullable = false)
    private double electricityRate; // Snapshot tại thời điểm xuất hóa đơn

    @Column(name = "old_water_index", nullable = false)
    private double oldWaterIndex;

    @Column(name = "new_water_index", nullable = false)
    private double newWaterIndex;

    @Column(name = "water_rate", nullable = false)
    private double waterRate; // Snapshot tại thời điểm xuất hóa đơn (hoặc đơn giá/người nếu cố định)

    @Column(name = "room_price", nullable = false)
    private double roomPrice; // Tiền phòng tính thực tế cho chu kỳ này (đã chia ngày lẻ nếu có)

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_type", nullable = false)
    private WaterBillingType waterBillingType; // Snapshot cách tính tiền nước lúc lập

    @Column(name = "number_of_tenants", nullable = false)
    private int numberOfTenants; // Snapshot số người lúc lập

    @Column(name = "contracted_room_price", nullable = false)
    private double contractedRoomPrice; // Snapshot đơn giá phòng gốc lúc lập

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private double paidAmount;

    @Column(name = "discount", nullable = false)
    private double discount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status; // PENDING, PAID, PARTIALLY_PAID

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
}
