package az.techvibeds.specialservice.dtos.constructionMaterial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConstructionProjectMaterialCreateDto {
    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotNull(message = "Warehouse is required")
    private Long warehouseId;

    @NotNull(message = "Required quantity is required")
    @Positive(message = "Required quantity must be greater than zero")
    private Integer requiredQuantity;

    private String note;
}
