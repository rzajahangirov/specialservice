package az.techvibeds.specialservice.dtos.semiProduct;

import az.techvibeds.specialservice.enums.SemiProductStatus;
import az.techvibeds.specialservice.enums.UnitType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemiProductUpdateDto {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 50)
    private String productCode;

    private String description;

    @NotNull(message = "Unit type is required")
    private UnitType unitType;

    @NotNull(message = "Product status is required")
    private SemiProductStatus productStatus;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    private BigDecimal price;
    private String vatStatus;
    private BigDecimal vatRate;
}
