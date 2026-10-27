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
public class LeaveRequestUpdateDto {
    @NotNull(message = "Start date is required")
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotBlank(message = "Reason is required")
    private String reason;
}
