package az.techvibeds.specialservice.dtos.product;


import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventoryDto {
    private Long id;
    private Long productId;
    private String productCode;
    private String name;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal marginPercentage;
    private BigDecimal discount;
    private String warehouse;
    private String category;
    private String unitName;
    private String productStatus;

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

