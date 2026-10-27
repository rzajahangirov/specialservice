package az.techvibeds.specialservice.dtos.productionOrder;

import az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductCreateDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderItemCreateDto {
    private Long productId;
    private Long materialProductId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Min(value = 0, message = "Material quantity cannot be negative")
    private Integer materialQuantity;

    @Valid
    @NotNull(message = "Product details are required")
    private ManufacturedProductCreateDto manufacturedProduct;

    private java.util.List<ProductionOrderIngredientDto> ingredients;
    private java.util.List<ProductionOrderExpenseDto> expenses;
}

