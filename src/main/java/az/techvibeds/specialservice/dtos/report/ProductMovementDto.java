package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Malın hərəkəti hesabatı — hər məhsul üçün giriş, çıxış, transfer və qalıq məlumatı.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductMovementDto {
    private Long productId;
    private String productName;
    private String productCode;
    private String categoryName;
    private String productType;       // PRODUCT / GOODS

    private Integer openingStock;     // Dövrün əvvəlindəki qalıq
    private Integer totalIncoming;    // Gələn miqdar (alış / istehsal)
    private Integer totalOutgoing;    // Gedən miqdar (satış / istifadə)
    private Integer totalTransfer;    // Anbarlar arası transfer
    private Integer closingStock;     // Dövrün sonundakı qalıq

    private BigDecimal unitPrice;      // Satış qiyməti (vahid)
    private BigDecimal totalValue;     // Qalıq dəyəri — satış qiymətinə görə (closingStock × unitPrice)

    private BigDecimal purchasePrice;  // Alış qiymeti (vahid)
    private BigDecimal totalCostValue; // Maya dəyəri — alış qiymətinə görə (closingStock × purchasePrice)
}
