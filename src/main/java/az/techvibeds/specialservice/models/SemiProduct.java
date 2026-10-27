package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.SemiProductStatus;
import az.techvibeds.specialservice.enums.UnitType;
import az.techvibeds.specialservice.enums.VatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "semi_products")
public class SemiProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String productCode;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType unit;

    private Long stock;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SemiProductStatus status;

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

    @OneToMany(mappedBy = "semiProduct")
    private List<WarehouseProduct> warehouseProducts = new ArrayList<>();

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

        if (vatStatus == VatStatus.WITHOUT_VAT) {
            this.priceWithVat = price;
            this.vatRate = null;
        } else if (vatRate != null) {
            this.priceWithVat = price.multiply(
                    BigDecimal.ONE.add(vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.priceWithVat = price;
        }
    }
}