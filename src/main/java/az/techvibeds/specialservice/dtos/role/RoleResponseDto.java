package az.techvibeds.specialservice.dtos.role;

import az.techvibeds.specialservice.enums.RoleStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private RoleStatus status;
}
