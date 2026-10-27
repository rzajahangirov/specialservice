package az.techvibeds.specialservice.dtos.companyStock;

import lombok.*;




@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyStockInventoryDto {
    private Long stockCount;
    private String growthRate;
}
