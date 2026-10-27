package az.techvibeds.specialservice.dtos.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminPasswordChangeDto {
    @NotBlank(message = "New password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;
}
