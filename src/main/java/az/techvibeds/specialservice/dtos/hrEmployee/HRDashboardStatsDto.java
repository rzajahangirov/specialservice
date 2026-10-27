package az.techvibeds.specialservice.dtos.hrEmployee;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HRDashboardStatsDto {
    private Long totalEmployees;
    private Long activeEmployees;
    private Long employeesOnLeave;
}
