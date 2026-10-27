package az.techvibeds.specialservice.dtos.company;

import az.techvibeds.specialservice.dtos.user.UserReadDto;
import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyReadDto {
    private Long id;
    private String name;
    private Date expressDate;

    private List<UserReadDto> users;
    private Set<ModuleReadDto> modules;
    private Boolean subscriptionFrozen;
}
