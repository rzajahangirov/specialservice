package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.AdditionalExpenseType;
import az.techvibeds.specialservice.enums.ProductType;
import az.techvibeds.specialservice.enums.VatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Faktura sətri (Invoice Item).
 * Hər sətir bir məhsul/xidmətə aiddir.
 * UI-da göründüyü kimi: Ad, Vahid, Qiymət, Miqdar, Barkod, Toplam.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Məhsul/Xidmət adı */
    @Column(nullable = false)
    private String name;

    /** Ölçü vahidi (ədəd, qutu, saat və s.) */
    private String unit;

    /** Vahid qiyməti */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    /** Miqdar */
    @Column(nullable = false)
    private Integer quantity;

    /** Barkod */
    private String barcode;

    /** Sətir toplam məbləği (unitPrice × quantity + vatAmount) */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /** Sətir üzrə ƏDV məbləği */
    @Column(precision = 19, scale = 2, nullable = false, columnDefinition = "numeric(19,2) default 0.00")
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_status")
    private VatStatus vatStatus = VatStatus.WITHOUT_VAT;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "prime_cost", precision = 19, scale = 2)
    private BigDecimal primeCost;

    /** Sətir üzrə endirim dəyəri (faiz və ya sabit məbləğ) */
    @Column(name = "discount", precision = 19, scale = 2)
    private BigDecimal discount;

    /** Endirim növü: AMOUNT (sabit məbləğ) və ya PERCENTAGE (faiz) */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private AdditionalExpenseType discountType;

    /** Hesablanmış endirim məbləği (avtomatik hesablanır) */
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Bu sətrin aid olduğu faktura */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** Əgər məhsul bazadan seçilibsə — opsional əlaqə */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /**
     * Sətir toplam məbləğini, endirim məbləğini və ƏDV məbləğini hesablayır.
     * Persist və ya update olunandan əvvəl avtomatik çağrılır.
     *
     * Hesablama ardıcıllığı:
     * 1. baseTotal = unitPrice × quantity
     * 2. discountAmount hesablanır (faiz və ya sabit)
     * 3. afterDiscount = baseTotal - discountAmount
     * 4. ƏDV afterDiscount üzərindən hesablanır
     * 5. lineTotal = afterDiscount + vatAmount
     */
    @PrePersist
    @PreUpdate
    public void calculateLineTotal() {
        if (unitPrice != null && quantity != null) {
            BigDecimal baseTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            // Endirim hesabla
            this.discountAmount = BigDecimal.ZERO;
            if (this.discount != null && this.discount.compareTo(BigDecimal.ZERO) > 0) {
                if (this.discountType == AdditionalExpenseType.PERCENTAGE) {
                    this.discountAmount = baseTotal.multiply(this.discount)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    this.discountAmount = this.discount;
                }
            }

            BigDecimal afterDiscount = baseTotal.subtract(this.discountAmount);
            if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) {
                afterDiscount = BigDecimal.ZERO;
                this.discountAmount = baseTotal;
            }

            // ƏDV hesabla (endirimdən sonra)
            if (vatStatus == VatStatus.WITH_VAT && vatRate != null) {
                this.vatAmount = afterDiscount.multiply(
                        vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                ).setScale(2, RoundingMode.HALF_UP);
                this.lineTotal = afterDiscount.add(this.vatAmount);
            } else {
                this.vatAmount = BigDecimal.ZERO;
                this.lineTotal = afterDiscount;
            }
        }
    }
}

