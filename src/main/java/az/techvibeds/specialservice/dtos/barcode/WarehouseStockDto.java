package az.techvibeds.specialservice.dtos.barcode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStockDto {
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
}
