package az.techvibeds.specialservice.dtos.productionOrder;

import az.techvibeds.specialservice.enums.ProductionOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderUpdateDto {
    private Long warehouseId;

    @NotNull(message = "Order status is required")
    private ProductionOrderStatus orderStatus;

    private Date startDate;
    private Date finishDate;

    @Min(0) @Max(100)
    private Integer progressPercentage;

    @Valid
    @NotEmpty(message = "At least one item is required in the order")
    private List<ProductionOrderItemUpdateDto> items;
}
