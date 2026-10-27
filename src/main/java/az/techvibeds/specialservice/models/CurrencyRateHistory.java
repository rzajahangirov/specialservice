package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "currency_rate_histories")
public class CurrencyRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal rate;

    @Column(nullable = false)
    private LocalDateTime rateDate;

    @PrePersist
    protected void onCreate() {
        this.rateDate = LocalDateTime.now();
    }
}
