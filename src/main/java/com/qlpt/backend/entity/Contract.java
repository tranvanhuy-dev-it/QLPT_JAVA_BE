package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private double deposit;

    @Column(name = "contracted_room_price", nullable = false)
    private double contractedRoomPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false)
    private BillingMode billingMode; // BY_RENTAL_DAYS, FIXED_DATE_OF_MONTH

    @Column(name = "fixed_billing_day")
    private Integer fixedBillingDay; // e.g. 5 (5th of every month)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status; // ACTIVE, EXPIRED, TERMINATED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(name = "number_of_tenants", nullable = false)
    private int numberOfTenants;
}
