package az.techvibeds.specialservice.dtos.hrEmployee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HREmployeeUpdateDto {
    @NotBlank(message = "First name is mandatory")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    private String lastName;

    @NotNull(message = "Hire date is mandatory")
    private LocalDate hireDate;

    private String phoneNumber;
    private String position;
}
