package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.ModuleRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_module_roles")
public class UserModuleRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private CompanyModule companyModule;

    @Enumerated(EnumType.STRING)
    private ModuleRole role;
}

