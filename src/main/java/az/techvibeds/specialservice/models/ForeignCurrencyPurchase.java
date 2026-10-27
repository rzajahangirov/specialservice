package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "foreign_currency_purchases")
public class ForeignCurrencyPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String supplierName; // Təchizatçı Adı

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount; // Alış Məbləği (xarici valyutada)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency; // Valyuta

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal exchangeRate; // Alış zamanı olan məzənnə

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal convertedAmount; // Yerli Məbləğ (AZN)

    @Column(precision = 19, scale = 2)
    private BigDecimal exchangeRateDifference; // Məzənnə Fərqi

    @Column(nullable = false)
    private LocalDate purchaseDate; // Tarix

    private String expenseCategory; // Xərcin Tanınması (Xərc növü)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Hansı şirkətə aiddir

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
