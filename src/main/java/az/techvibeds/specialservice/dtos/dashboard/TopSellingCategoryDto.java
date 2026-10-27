package az.techvibeds.specialservice.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopSellingCategoryDto {
    private String categoryName;
    private Long quantitySold;
}
