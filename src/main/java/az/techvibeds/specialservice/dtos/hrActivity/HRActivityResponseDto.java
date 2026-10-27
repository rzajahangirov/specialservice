package az.techvibeds.specialservice.dtos.hrActivity;

import az.techvibeds.specialservice.enums.ActivityStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HRActivityResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private String activityType;
    private LocalDateTime createdAt;
    private ActivityStatus status;
}
