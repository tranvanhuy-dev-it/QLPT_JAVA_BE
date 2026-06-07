package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "contract_addendum_extra_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ContractAddendumExtraFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addendum_id", nullable = false)
    private ContractAddendum addendum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extra_fee_id", nullable = false)
    private ExtraFee extraFee;

    @Column(name = "custom_price", nullable = false)
    private double customPrice;
}
