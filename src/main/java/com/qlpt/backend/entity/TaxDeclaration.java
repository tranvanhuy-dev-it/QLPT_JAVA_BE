package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_declarations", indexes = {
    @Index(name = "idx_tax_declarations_landlord_id", columnList = "landlord_id"),
    @Index(name = "idx_tax_declarations_boarding_house_id", columnList = "boarding_house_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaxDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_house_id")
    private BoardingHouse boardingHouse; // Can be null if declaring for all boarding houses

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "period_type", nullable = false)
    private String periodType; // "MONTH", "QUARTER", "YEAR"

    @Column(name = "period_value", nullable = false)
    private int periodValue; // Month (1-12), Quarter (1-4), or 0 for whole year

    @Column(name = "total_revenue", nullable = false)
    private double totalRevenue; // Sum of paid invoices in the period

    @Column(name = "vat_amount", nullable = false)
    private double vatAmount;

    @Column(name = "pit_amount", nullable = false)
    private double pitAmount;

    @Column(name = "total_tax_amount", nullable = false)
    private double totalTaxAmount;

    @Column(name = "declaration_number", unique = true, nullable = false)
    private String declarationNumber; // MST-YYYYMMDD-XXXXXX

    @Column(name = "status", nullable = false)
    private String status; // "SUBMITTED", "APPROVED"

    @Column(name = "submitted_date", nullable = false)
    private LocalDateTime submittedDate;

    @Column(name = "tax_authority_response", columnDefinition = "TEXT")
    private String taxAuthorityResponse;
}
