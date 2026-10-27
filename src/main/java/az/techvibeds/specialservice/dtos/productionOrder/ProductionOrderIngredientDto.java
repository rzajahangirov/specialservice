package az.techvibeds.specialservice.dtos.productionOrder;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductionOrderIngredientDto {
    private Long id;

    private Long productId;
    private String productName;

    @NotNull(message = "Warehouse ID is required for each ingredient")
    private Long warehouseId;
    private String warehouseName;

    @NotNull(message = "Ingredient quantity is required")
    private BigDecimal quantity;
}
