package az.techvibeds.specialservice.dtos.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "InventoryCategoryCreateDto")
public class CategoryCreateDto {
    private String name;
}
