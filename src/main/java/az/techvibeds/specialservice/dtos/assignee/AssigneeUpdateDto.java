package az.techvibeds.specialservice.dtos.assignee;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssigneeUpdateDto {
    @NotNull(message = "ID is required")
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Min(value = 0, message = "Active service count cannot be negative")
    private Integer activeServiceCount;

    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;
}
