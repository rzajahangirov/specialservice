package az.techvibeds.specialservice.dtos.module;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleCreateDto {
    private String name;
    private String description;
    private String icon;
    private String backendKey; // Məsələn: HR, FINANCE, INVOICE, WAREHOUSE...
}



