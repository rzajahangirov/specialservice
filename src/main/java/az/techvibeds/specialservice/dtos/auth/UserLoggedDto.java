package az.techvibeds.specialservice.dtos.auth;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import az.techvibeds.specialservice.dtos.moduleRole.UserModulePermissionDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoggedDto {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private List<String> roles;
    private List<UserModulePermissionDto> permissions;
}
