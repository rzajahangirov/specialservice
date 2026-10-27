package az.techvibeds.specialservice.dtos.projectContractor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContractorDto {
    private Long id;
    private String name;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private Integer projectCount;
}
