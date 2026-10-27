package az.techvibeds.specialservice.dtos.service;

import az.techvibeds.specialservice.enums.ServiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceUpdateDto {
    @NotNull(message = "ID is required")
    private Long id;
    private String name;
    private String description;
    private java.math.BigDecimal amount;
    private LocalDate deadline;
    private Long unitId;
    private Long partnerId;
    private ServiceStatus status;
    private Long assigneeId;
}
