package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.AdditionalExpenseType;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.enums.PaymentMethod;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Faktura (Invoice) entity.
 * Hər faktura bir partner-ə (müştəri və ya təchizatçı) bağlıdır
 * və içərisində bir neçə faktura sətri (InvoiceItem) ola bilər.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoices")
@SQLDelete(sql = "UPDATE invoices SET is_deleted = true WHERE id=? and version=?")
@SQLRestriction("is_deleted=false")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    /**  Faktura nömrəsi (məs: INV-2024-001) */
    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    /** Faktura növü: SALES (satış) və ya PURCHASE (alış) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType invoiceType;

    /** Faktura statusu */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    /** Faktura tarixi */
    @Column(nullable = false)
    private LocalDate invoiceDate;

    /** Çatdırılma tarixi */
    private LocalDate deliveryDate;

    /** Ödəniş metodu */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** Valyuta (AZN, USD, EUR və s.) */
    @Column(nullable = false)
    private String currency = "AZN";

    /** Ümumi məbləğ — faktura sətirlərindən hesablanır */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Ümumi ƏDV məbləği — faktura sətirlərindən hesablanır */
    @Column(precision = 19, scale = 2, nullable = false, columnDefinition = "numeric(19,2) default 0.00")
    private BigDecimal totalVatAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false, columnDefinition = "numeric(19,2) default 0.00")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false, columnDefinition = "numeric(19,2) default 0.00")
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false, columnDefinition = "numeric(19,2) default 0.00")
    private BigDecimal additionalExpense = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'AMOUNT'")
    private AdditionalExpenseType additionalExpenseType = AdditionalExpenseType.AMOUNT;

    @Column(precision = 19, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private AdditionalExpenseType discountType = AdditionalExpenseType.AMOUNT;

    private Boolean stockApplied = false;

    /** Müştəri və ya təchizatçı */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /** Şirkət — bu faktura hansı şirkətə aiddir */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Faktura sətirləri (məhsul/xidmət) */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    /** Qeyd / əlavə məlumat */
    @Column(length = 500)
    private String notes;

    /** Yaradılma vaxtı */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Son yenilənmə vaxtı */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.additionalExpense == null) {
            this.additionalExpense = BigDecimal.ZERO;
        }
        if (this.additionalExpenseType == null) {
            this.additionalExpenseType = AdditionalExpenseType.AMOUNT;
        }
        if (this.discount == null) {
            this.discount = BigDecimal.ZERO;
        }
        if (this.discountType == null) {
            this.discountType = AdditionalExpenseType.AMOUNT;
        }
        this.remainingAmount = this.totalAmount.subtract(this.paidAmount);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ——— Yardımçı metodlar ———

    /** Fakturaya yeni sətir əlavə edir və ümumi məbləği yeniləyir */
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
        recalculateTotal();
    }

    /** Fakturadan sətir silir və ümumi məbləği yeniləyir */
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
        recalculateTotal();
    }

    /** Bütün sətirlərin toplam məbləğini və ƏDV məbləğini hesablayır */
    public void recalculateTotal() {
        BigDecimal baseTotal = items.stream()
                .map(InvoiceItem::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalVatAmount = items.stream()
                .map(InvoiceItem::getVatAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.discount == null) {
            this.discount = BigDecimal.ZERO;
        }
        if (this.discountType == null) {
            this.discountType = AdditionalExpenseType.AMOUNT;
        }

        BigDecimal discountAmt = BigDecimal.ZERO;
        if (this.discount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.discountType == AdditionalExpenseType.PERCENTAGE) {
                discountAmt = baseTotal.multiply(this.discount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                discountAmt = this.discount;
            }
        }

        this.totalAmount = baseTotal.subtract(discountAmt);
        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }

        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        this.remainingAmount = this.totalAmount.subtract(this.paidAmount);

        // ——— Prime Cost (Maya dəyəri) hesablanması (yalnız PURCHASE üçün) ———
        if (this.invoiceType == InvoiceType.PURCHASE) {
            BigDecimal totalBaseAmount = calculateTotalBaseAmount();

            BigDecimal exp = BigDecimal.ZERO;
            if (this.additionalExpense != null && this.additionalExpense.compareTo(BigDecimal.ZERO) > 0) {
                if (this.additionalExpenseType == AdditionalExpenseType.PERCENTAGE) {
                    exp = totalBaseAmount.multiply(this.additionalExpense).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    exp = this.additionalExpense;
                }
            }

            if (totalBaseAmount.compareTo(BigDecimal.ZERO) > 0 && exp.compareTo(BigDecimal.ZERO) > 0) {
                // Əmsal = xərc / cəm
                BigDecimal factor = exp.divide(totalBaseAmount, 10, RoundingMode.HALF_UP);
                for (InvoiceItem item : items) {
                    if (item.getUnitPrice() != null) {
                        BigDecimal pCost = item.getUnitPrice().multiply(BigDecimal.ONE.add(factor))
                                .setScale(2, RoundingMode.HALF_UP);
                        item.setPrimeCost(pCost);
                    } else {
                        item.setPrimeCost(BigDecimal.ZERO);
                    }
                }
            } else {
                for (InvoiceItem item : items) {
                    item.setPrimeCost(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
                }
            }
        } else {
            for (InvoiceItem item : items) {
                item.setPrimeCost(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
            }
        }
    }

    public BigDecimal calculateTotalBaseAmount() {
        return items.stream()
                .map(item -> {
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
