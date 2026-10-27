package az.techvibeds.specialservice.dtos.productionOrder;

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
public class ProductionOrderReadDto {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private String orderStatus;
    private Date startDate;
    private Date finishDate;
    private Integer progressPercentage;
    private List<ProductionOrderItemReadDto> items;
}
