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
public class ProductInventoryUpdateDto {
    private Long warehouseProductId;
    private String productCode;
    private String name;
    private Integer quantity;
    private Long warehouseId;
    private Long categoryId;
    private Long unitId;
    private String productStatus;

    /** Məhsul tipi: PRODUCT / GOODS */
    private String productType;

    private Long minStockLimit;

    private BigDecimal price;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal marginPercentage;
    private BigDecimal discount;
    private String vatStatus;
    private BigDecimal vatRate;
}
