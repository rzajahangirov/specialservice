package az.techvibeds.specialservice.dtos.hrContract;

import az.techvibeds.specialservice.enums.ContractType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponseDto {
    private Long id;
    private Long employeeId;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal salary;
    private String documentUrl;
    private boolean isActive;
}
