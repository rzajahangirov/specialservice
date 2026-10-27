package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.enums.ProductType;
import az.techvibeds.specialservice.enums.VatStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted=false")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    private String name;
    private String productCode;
    private Long stock;

    @Column(name = "min_stock_limit", columnDefinition = "bigint default 0")
    private Long minStockLimit = 0L;

    /** ƏDV-siz qiymət (həm məhsul, həm mal üçün əsas qiymət) */
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal marginPercentage;

    @Column(precision = 19, scale = 2)
    private BigDecimal discount;

    /** ƏDV statusu: WITH_VAT (ƏDV-li), WITHOUT_VAT (ƏDV-siz) */
    @Enumerated(EnumType.STRING)
    private VatStatus vatStatus = VatStatus.WITHOUT_VAT;

    /** ƏDV faizi (həm PRODUCT, həm də GOODS üçün: 0 və ya 18) */
    @Column(precision = 5, scale = 2)
    private BigDecimal vatRate;

    /** ƏDV-li qiymət (avtomatik hesablanır: price * (1 + vatRate/100)) */
    @Column(precision = 19, scale = 2)
    private BigDecimal priceWithVat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType = ProductType.GOODS;

    @OneToMany(mappedBy = "product")
    private List<WarehouseProduct> warehouseProducts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    private Company company;

    /**
     * Həm PRODUCT, həm də GOODS tipli məhsullar üçün ƏDV-li qiyməti avtomatik hesablayır.
     * Əgər price və vatRate mövcuddursa: priceWithVat = price * (1 + vatRate / 100)
     */
    @PrePersist
    @PreUpdate
    public void calculatePriceWithVat() {
        if (this.price != null && this.vatRate != null
                && this.vatRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal multiplier = BigDecimal.ONE.add(
                    this.vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.priceWithVat = this.price.multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            this.vatStatus = VatStatus.WITH_VAT;
        } else {
            this.priceWithVat = this.price;
            this.vatStatus = VatStatus.WITHOUT_VAT;
        }

        // Stok statusunun avtomatik yenilənməsi
        if (this.stock == null || this.stock <= 0) {
            this.productStatus = ProductStatus.INACTIVE; // Aktiv deyil (Stokda yoxdur)
        } else if (this.minStockLimit != null && this.stock <= this.minStockLimit) {
            this.productStatus = ProductStatus.LOW_STOCK; // Azalan
        } else {
            this.productStatus = ProductStatus.IN_STOCK; // Aktiv (Stokda var)
        }
    }
}
