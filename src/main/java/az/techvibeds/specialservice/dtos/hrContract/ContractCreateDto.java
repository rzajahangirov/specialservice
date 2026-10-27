package az.techvibeds.specialservice.dtos.hrContract;

import az.techvibeds.specialservice.enums.ContractType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCreateDto {
    @NotNull(message = "Contract type is mandatory")
    private ContractType contractType;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    private LocalDate endDate;

    private String documentUrl;

    private BigDecimal salary;

    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
