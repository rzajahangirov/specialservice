package az.techvibeds.specialservice.dtos.deduction;

import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.enums.DeductionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionCreateDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Deduction type is required")
    private DeductionType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Deduction amount must be greater than zero")
    private BigDecimal amount;

    private String reason;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1")
    private Integer duration;

    @NotNull(message = "Duration type is required")
    private DeductionDurationType durationType;
}