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
public class ContractDetailDto {
    private Long id;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String documentUrl;
    private BigDecimal salary;
    private boolean isActive;
}
