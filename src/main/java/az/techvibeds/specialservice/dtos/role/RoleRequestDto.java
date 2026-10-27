package az.techvibeds.specialservice.dtos.role;

import az.techvibeds.specialservice.enums.RoleStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequestDto {
    @NotBlank(message = "Rol adı boş ola bilməz")
    private String name;
    private String description;
    private RoleStatus status;
}