package az.techvibeds.specialservice.dtos.leaveRequest;

import az.techvibeds.specialservice.enums.LeaveType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestCreateDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType; // ANNUAL, SICK, UNPAID

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}
