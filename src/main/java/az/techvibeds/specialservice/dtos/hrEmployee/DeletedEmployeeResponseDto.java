package az.techvibeds.specialservice.dtos.hrEmployee;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedEmployeeResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private EmploymentStatus status;
    private LocalDate hireDate;
    private LocalDateTime deletedAt;
}
