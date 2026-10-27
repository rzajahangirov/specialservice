package az.techvibeds.specialservice.dtos.assignee;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssigneeCreateDto {
    @NotBlank(message = "Name can not be null")
    private String name;

    @NotNull(message = "Capacity is important")
    @Min(value = 1, message = "Min is 1")
    private Integer totalCapacity;
}
