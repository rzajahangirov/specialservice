package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecurringExpenseReadDto {
    private Long id;
    private String name;
    private BigDecimal amount;
    private String description;
    private String frequency;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private LocalDate lastExecutedAt;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
