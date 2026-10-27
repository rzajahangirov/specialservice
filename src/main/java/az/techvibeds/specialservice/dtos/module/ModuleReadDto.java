package az.techvibeds.specialservice.dtos.module;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleReadDto {
    private Long id;
    private String name;
    private String icon;
    private String backendKey; // Frontend bu key ilə hansı route-u açacağını bilir
}

