package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Xarici Satınalma üçün sıra nömrəsi sayğacı.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "foreign_purchase_sequences", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "year"}))
public class ForeignPurchaseSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Long lastNumber = 0L;

    public String generateNextNumber() {
        this.lastNumber++;
        return String.format("F-PUR-%d-%03d", this.year, this.lastNumber);
    }
}
