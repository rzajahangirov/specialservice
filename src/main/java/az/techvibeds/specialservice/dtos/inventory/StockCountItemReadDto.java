package az.techvibeds.specialservice.dtos.inventory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockCountItemReadDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private Integer expectedQuantity;
    private Integer countedQuantity;
    private Integer difference;
}
