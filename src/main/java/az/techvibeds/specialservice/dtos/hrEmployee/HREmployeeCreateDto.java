package az.techvibeds.specialservice.dtos.hrEmployee;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HREmployeeCreateDto {
    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    private String position;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Employment status is mandatory")
    private EmploymentStatus status;

    @NotNull(message = "Hire date is mandatory")
    private LocalDate hireDate;
}
