package az.techvibeds.specialservice.dtos.barcode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarcodeSearchResultDto {
    private Long id;
    private String name;
    private String barcode;
    private String type; // PRODUCT, GOODS, SEMI_PRODUCT
    private BigDecimal price;
    private BigDecimal priceWithVat;
    private BigDecimal vatRate;
    private String vatStatus;
    private String unit;
    private String category;
    private String status;
    private Long minStockLimit;
    private Long totalStock;
    private List<WarehouseStockDto> warehouseStocks;
}
