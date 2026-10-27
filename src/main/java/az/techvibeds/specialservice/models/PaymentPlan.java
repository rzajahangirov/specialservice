package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment_plans")
public class PaymentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Plan Adı

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount; // Ümumi Məbləğ

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO; // Ödənilmiş Məbləğ

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal remainingAmount; // Qalan Məbləğ

    private LocalDate lastPaymentDate; // Son Ödəniş Tarixi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Installment> installments = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalculateRemaining();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateRemaining();
    }

    public void recalculateRemaining() {
        if (totalAmount != null) {
            this.remainingAmount = totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
        }
    }
}
