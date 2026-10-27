package az.techvibeds.specialservice.dtos.service;


import az.techvibeds.specialservice.enums.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceExecutionStatusDto {
    private Long id;
    private String name;
    private String dtoAssignee;
    private ServiceStatus status;
    private LocalDate deadline;
}
