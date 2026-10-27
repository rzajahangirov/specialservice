package az.techvibeds.specialservice.dtos.projectContractor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContractorUpdateDto {
    private String name;
    private String contactPerson;
    private String phoneNumber;
    private String email;
}

