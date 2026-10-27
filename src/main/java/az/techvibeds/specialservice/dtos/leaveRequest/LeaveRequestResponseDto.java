package az.techvibeds.specialservice.dtos.leaveRequest;

import az.techvibeds.specialservice.enums.ActivityStatus;
import az.techvibeds.specialservice.enums.LeaveType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveType leaveType;
    private ActivityStatus status;
    private String reason;
}
