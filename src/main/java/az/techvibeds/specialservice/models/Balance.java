package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "balance")
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String currencyType;
    private BigDecimal amount;

    @OneToOne(mappedBy = "balance", fetch = FetchType.LAZY)
    private Partner partner;
}
