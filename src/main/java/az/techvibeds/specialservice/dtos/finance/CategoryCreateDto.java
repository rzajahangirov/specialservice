package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(name = "FinanceCategoryCreateDto")
public class CategoryCreateDto {
    private String name;
    private String type; // INCOME or EXPENSE
}
