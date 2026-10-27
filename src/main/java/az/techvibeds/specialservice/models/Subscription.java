package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.BillingCycle;
import az.techvibeds.specialservice.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; 

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle; 

    private LocalDate startDate; 
    private LocalDate endDate; 

    @Column(precision = 19, scale = 2)
    private BigDecimal pricePaid; // Ne qeder odenis edilib

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean frozen = false; // Subscription expire olduqda true olur, rollar dondurulur

    private LocalDate frozenAt; // Nə vaxt dondurulub
}
