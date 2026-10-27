package az.techvibeds.specialservice.dtos.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private Boolean active;
}
