package az.techvibeds.specialservice.dtos.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReadDto {
    private Long id;
    private Long warehouseProductId;
    private Long productId;
    private String name;
    private String productCode;
    private Long totalStock;
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal marginPercentage;
    private BigDecimal discount;
    private String categoryName;
    private String unitName;
    private String productStatus;
    private String warehouseName;
    private Integer quantity;

    /** ƏDV statusu: WITH_VAT / WITHOUT_VAT */
    private String vatStatus;
    /** ƏDV faizi */
    private BigDecimal vatRate;
    /** ƏDV-li qiymət */
    private BigDecimal priceWithVat;
    /** Məhsul tipi: PRODUCT / GOODS */
    private String productType;
    private Long minStockLimit;
}

