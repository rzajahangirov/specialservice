package az.techvibeds.specialservice.dtos.inventory;

import lombok.Data;
import java.util.List;

@Data
public class StockCountCreateDto {
    private Long warehouseId;
    private String note;
    private List<StockCountItemDto> items;
}
