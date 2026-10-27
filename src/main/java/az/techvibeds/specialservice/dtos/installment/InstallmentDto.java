package az.techvibeds.specialservice.dtos.installment;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InstallmentDto {
    private Long id;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private boolean paid;
}
