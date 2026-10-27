package az.techvibeds.specialservice.dtos.service;

import az.techvibeds.specialservice.enums.ServiceStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCreateDto {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    private String description;

    @NotNull(message = "Amount cannot be empty")
    private java.math.BigDecimal amount;

    @FutureOrPresent(message = "Deadline cannot be in the past")
    private LocalDate deadline;

    @NotNull(message = "Unit ID is required")
    private Long unitId;

    @NotNull(message = "Partner ID is required")
    private Long partnerId;

    @NotNull(message = "Status is required")
    private ServiceStatus status;

    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;
}