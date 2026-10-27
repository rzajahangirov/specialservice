package az.techvibeds.specialservice.dtos.hrEmployee;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateDto {
    @NotNull(message = "Status is mandatory")
    private EmploymentStatus status;
}
