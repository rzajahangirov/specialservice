package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductMovementDetailReportDto {
    private Long activityId;
    private Long productId;
    private String productName;
    private String productCode;
    private String categoryName;
    private String activityType; // GİRİŞ, ÇIXIŞ, TRANSFER
    private Integer quantity;

    private String sourceName; // Haradan çıxdı (anbar adı, təchizatçı adı və s.)
    private String destinationName; // Hara getdi / Təyinat (anbar adı, müştəri adı, tikinti layihəsi və s.)
    private String referenceNumber; // Qaimə №, Transfer №, İstehsal №
    private LocalDateTime date; // Hərəkət tarixi

    private BigDecimal purchasePrice; // Alış qiyməti (Maya)
    private BigDecimal salePrice; // Satış qiyməti
    private BigDecimal totalPurchaseValue; // quantity * purchasePrice
    private BigDecimal totalSaleValue; // quantity * salePrice
}
