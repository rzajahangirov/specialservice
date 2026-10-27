package az.techvibeds.specialservice.dtos.user;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemUserResponseDto {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private Boolean active;
    private Long companyId;
    private String companyName;
    private List<String> roles;
}
