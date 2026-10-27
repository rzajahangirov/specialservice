package az.techvibeds.specialservice.dtos.hrActivity;

import az.techvibeds.specialservice.enums.ActivityStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HRActivityCreateDto {
    @NotNull
    private Long employeeId;

    @NotBlank
    private String activityType;

    @NotNull
    private ActivityStatus status;
}
