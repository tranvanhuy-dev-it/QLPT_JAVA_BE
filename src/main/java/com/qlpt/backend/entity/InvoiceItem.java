package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_items_invoice_id", columnList = "invoice_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String name; // e.g. "Phụ phí Wifi", "Phí rác"

    @Column(nullable = false)
    private double price; // Đơn giá tại thời điểm xuất hóa đơn

    @Column(nullable = false)
    private double quantity; // Số lượng (ví dụ số người ở hoặc 1 phòng)

    @Column(nullable = false)
    private double subtotal; // Thành tiền (price * quantity)
}
