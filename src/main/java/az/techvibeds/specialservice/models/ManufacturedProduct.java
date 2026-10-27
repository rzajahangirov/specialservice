package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.VatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "manufactured_products")
public class ManufacturedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal primeCost;

    /** ƏDV-siz qiymət */
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    /** ƏDV statusu: WITH_VAT (ƏDV-li), WITHOUT_VAT (ƏDV-siz) */
    @Enumerated(EnumType.STRING)
    private VatStatus vatStatus = VatStatus.WITHOUT_VAT;

    /** ƏDV faizi (0 və ya 18) */
    @Column(precision = 5, scale = 2)
    private BigDecimal vatRate;

    /** ƏDV-li qiymət */
    @Column(precision = 19, scale = 2)
    private BigDecimal priceWithVat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @PrePersist
    @PreUpdate
    public void calculatePriceWithVat() {
        if (price == null) {
            this.priceWithVat = null;
            return;
        }

        if (vatStatus == VatStatus.WITHOUT_VAT || vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0) {
            this.priceWithVat = price;
        } else {
            this.priceWithVat = price.multiply(
                    BigDecimal.ONE.add(vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);
        }
    }
}