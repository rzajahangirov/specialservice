package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Faktura nömrəsi ardıcıllığı.
 * Hər şirkət üçün ayrıca sayğac saxlayır.
 * Məsələn: INV-2024-001, INV-2024-002, ...
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice_sequences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "year"}))
public class InvoiceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hansı şirkətə aid olduğu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** İl (hər il sayğac sıfırlanır) */
    @Column(nullable = false)
    private Integer year;

    /** Cari sıra nömrəsi */
    @Column(nullable = false)
    private Long lastNumber = 0L;

    /**
     * Növbəti nömrəni generasiya edir.
     * Məsələn: INV-1-2024-001 formatında.
     */
    public String generateNextNumber() {
        this.lastNumber++;
        return String.format("INV-%d-%d-%03d", this.company.getId(), this.year, this.lastNumber);
    }
}
