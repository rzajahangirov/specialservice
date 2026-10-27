package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.StockCountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stock_counts")
@Builder
public class StockCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String countNumber; // Məsələn: SC-20240508-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User countedBy;

    private LocalDateTime countDate;

    @Enumerated(EnumType.STRING)
    private StockCountStatus status;

    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCountItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    public void addItem(StockCountItem item) {
        items.add(item);
        item.setStockCount(this);
    }
}
