package az.techvibeds.specialservice.dtos.installment;

import az.techvibeds.specialservice.enums.PlanStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentPlanReadDto {
    private Long id;
    private String name;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LocalDate lastPaymentDate;
    private PlanStatus status;
    private List<InstallmentDto> installments;
    private LocalDateTime createdAt;
}
