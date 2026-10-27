package az.techvibeds.specialservice.dtos.foreignPurchase;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ForeignPurchaseItemDto {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private String barcode;
    private BigDecimal lineTotal;
    private Long productId;
}
