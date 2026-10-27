package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(name = "FinanceCategoryReadDto")
public class CategoryReadDto {
    private Long id;
    private String name;
    private String type;
}
