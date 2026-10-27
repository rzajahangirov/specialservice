package az.techvibeds.specialservice.dtos.auth;

import az.techvibeds.specialservice.dtos.moduleRole.UserModulePermissionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponseDto {
    private String accessToken;
    private String token;
    private List<UserModulePermissionDto> permissions;
}