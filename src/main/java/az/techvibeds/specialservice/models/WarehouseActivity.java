package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "warehouse_activities")
public class WarehouseActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ActivityTypeWarehouse type;

    @ManyToOne(fetch = FetchType.LAZY)
    private WarehouseProduct product;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    private Warehouse sourceWarehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    private Warehouse destinationWarehouse;

    @Column(name = "purchase_price", precision = 19, scale = 2)
    private java.math.BigDecimal purchasePrice;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private java.math.BigDecimal salePrice;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "destination_name", length = 255)
    private String destinationName;

    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    private Company company;

}
