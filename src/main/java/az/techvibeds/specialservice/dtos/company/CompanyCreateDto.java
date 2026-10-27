package az.techvibeds.specialservice.dtos.company;

import lombok.*;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCreateDto {
    private String name;
    private Date expressDate;
    private Set<Long> moduleIds;
}
