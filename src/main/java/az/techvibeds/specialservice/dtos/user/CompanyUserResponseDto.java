package az.techvibeds.specialservice.dtos.user;

import az.techvibeds.specialservice.dtos.moduleRole.UserModulePermissionDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUserResponseDto {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private List<UserModulePermissionDto> permissions;
}
