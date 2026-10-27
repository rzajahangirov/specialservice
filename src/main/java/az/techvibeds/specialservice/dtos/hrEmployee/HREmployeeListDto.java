package az.techvibeds.specialservice.dtos.hrEmployee;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HREmployeeListDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private EmploymentStatus status;
}
