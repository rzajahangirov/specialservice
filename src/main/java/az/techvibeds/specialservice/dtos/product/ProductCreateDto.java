package az.techvibeds.specialservice.dtos.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateDto {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters")
    private String name;

    @Size(max = 50, message = "Product code cannot exceed 50 characters")
    private String productCode;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Product status must be selected")
    private String productStatus;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Unit ID is required")
    private Long unitId;

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