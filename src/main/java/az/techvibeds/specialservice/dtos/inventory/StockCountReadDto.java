package az.techvibeds.specialservice.dtos.inventory;

import az.techvibeds.specialservice.enums.StockCountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StockCountReadDto {
    private Long id;
    private String countNumber;
    private Long warehouseId;
    private String warehouseName;
    private String countedByEmail;
    private LocalDateTime countDate;
    private StockCountStatus status;
    private String note;
    private List<StockCountItemReadDto> items;
}
