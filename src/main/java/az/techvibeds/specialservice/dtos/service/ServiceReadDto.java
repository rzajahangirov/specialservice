package az.techvibeds.specialservice.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceReadDto {
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDate deadline;
    private String unit;
    private Long partnerId;
    private String partnerName;
    private String status;
    private String assignee;
    private String companyName;
    private BigDecimal paidAmount;
    private boolean isPaid;
}
