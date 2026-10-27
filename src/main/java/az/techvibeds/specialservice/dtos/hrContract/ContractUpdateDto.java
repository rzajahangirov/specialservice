package az.techvibeds.specialservice.dtos.hrContract;

import az.techvibeds.specialservice.enums.ContractType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractUpdateDto {
    @NotNull(message = "Contract type is mandatory")
    private ContractType contractType;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    private LocalDate endDate;

    private String documentUrl;

    @NotNull(message = "Salary is mandatory")
    private BigDecimal salary;

    @NotNull(message = "Active status is mandatory")
    private Boolean isActive;

    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
