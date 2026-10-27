package az.techvibeds.specialservice.dtos.installment;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaymentPlanCreateDto {
    private String name;
    private BigDecimal totalAmount;
    private List<InstallmentDto> installments;
}
