package az.techvibeds.specialservice.dtos.module;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleUpdateDto {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String backendKey;
}


