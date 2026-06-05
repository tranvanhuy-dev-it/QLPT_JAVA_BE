package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private double paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status; // PENDING, PAID, PARTIALLY_PAID

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
}
