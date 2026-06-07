package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contracts", indexes = {
    @Index(name = "idx_contracts_room_id", columnList = "room_id"),
    @Index(name = "idx_contracts_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_contracts_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ContractAddendum> addendums = new ArrayList<>();

    public ContractAddendum getLatestAddendum() {
        if (addendums == null || addendums.isEmpty()) {
            return null;
        }
        return addendums.stream()
            .max(java.util.Comparator.comparing(ContractAddendum::getStartDate))
            .orElse(null);
    }
}
