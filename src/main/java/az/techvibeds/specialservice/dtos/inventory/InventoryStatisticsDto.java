package az.techvibeds.specialservice.dtos.inventory;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStatisticsDto {
    @Builder.Default
    private Long stockCount = 0L;

    private String growthRate;

    @Builder.Default
    private Integer productCount = 0;

    @Builder.Default
    private Long activeWarehouseCount = 0L;

    private String lastTransfer;
}
