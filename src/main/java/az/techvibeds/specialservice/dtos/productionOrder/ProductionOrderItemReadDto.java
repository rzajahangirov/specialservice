package az.techvibeds.specialservice.dtos.productionOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderItemReadDto {
    private Long id;
    private String productName;
    private Long inventoryProductId;
    private String inventoryProductName;
    private String inventoryProductCode;
    private Long materialProductId;
    private String materialProductName;
    private String materialProductCode;
    private Integer quantity;
    private Integer materialQuantity;
    private Boolean stockApplied;
    private java.math.BigDecimal primeCost;
    private java.util.List<ProductionOrderIngredientDto> ingredients;
    private java.util.List<ProductionOrderExpenseDto> expenses;

    // VAT fields
    private java.math.BigDecimal price;
    private String vatStatus;
    private java.math.BigDecimal vatRate;
    private java.math.BigDecimal priceWithVat;
}

