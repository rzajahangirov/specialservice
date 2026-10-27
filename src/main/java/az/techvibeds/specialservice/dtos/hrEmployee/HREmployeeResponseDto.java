package az.techvibeds.specialservice.dtos.hrEmployee;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HREmployeeResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private EmploymentStatus status;
    private LocalDate hireDate;
    private String imageUrl;
}
