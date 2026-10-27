package az.techvibeds.specialservice.dtos.inventory;

import lombok.Data;

@Data
public class StockCountItemDto {
    private Long productId;
    private Integer countedQuantity;
    // Expected quantity will be fetched from system during creation
}
