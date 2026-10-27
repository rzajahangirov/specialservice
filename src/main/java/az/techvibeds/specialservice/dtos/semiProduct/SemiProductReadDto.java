package az.techvibeds.specialservice.dtos.semiProduct;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemiProductReadDto {
    private Long id;
    private Long warehouseProductId;
    private Long semiProductId;
    private String name;
    private String productCode;
    private String description;
    private Long totalStock;
    private BigDecimal price;
    private String categoryName;
    private String unitType;
    private String productStatus;
    private String warehouseName;
    private Integer quantity;

    /** ƏDV statusu: WITH_VAT / WITHOUT_VAT */
    private String vatStatus;
    /** ƏDV faizi */
    private BigDecimal vatRate;
    /** ƏDV-li qiymət */
    private BigDecimal priceWithVat;
}
