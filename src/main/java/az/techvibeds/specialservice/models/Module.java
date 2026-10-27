package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "modules")
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String icon; // UI-da menyuda göstəriləcək icon adı (məs: "users", "dollar-sign")

    /**
     * Sistemdəki sabit açar — frontend route və backend icəzələri bu key üzərindən əlaqələnir.
     * Nəmunə: HR, FINANCE, INVOICE, WAREHOUSE, PRODUCTION, CONSTRUCTION, SERVICE
     * Admin dəyişdərə bilməz — developer tərəfindən təyin edilir.
     */
    @Column(unique = true)
    private String backendKey;
}