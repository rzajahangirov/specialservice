package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.PartnerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Musteriler ve Techizatcilar
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "partners")
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String voen;
    
    private String contractNumber; // Muqavile nomresi (nullable)

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_id")
    private Balance balance;
    @Enumerated(EnumType.STRING)
    private PartnerType partnerType ;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    private Boolean isActive = true;


}
