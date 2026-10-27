package az.techvibeds.specialservice.dtos.deduction;

import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.enums.DeductionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionResponseDto {
    private Long id;
    private Long employeeId;
    private DeductionType type;
    private BigDecimal amount;
    private String reason;
    private LocalDate startDate;
    private Integer duration;
    private DeductionDurationType durationType;
    private boolean isActive;
}