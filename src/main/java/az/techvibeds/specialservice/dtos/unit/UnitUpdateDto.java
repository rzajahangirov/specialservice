package az.techvibeds.specialservice.dtos.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitUpdateDto {
    @NotNull(message = "Unit ID is required")
    private Long id;

    @NotBlank(message = "Unit name cannot be empty")
    private String name;

    private String category;
}
