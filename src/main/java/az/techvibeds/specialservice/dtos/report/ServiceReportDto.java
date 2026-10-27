package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceReportDto {
    private Long serviceId;
    private String serviceName;
    private String description;
    private java.math.BigDecimal amount;
    private String unitName;
    private String partnerName;
    private String status;
    private String assigneeName;
    private LocalDate deadline;
}
