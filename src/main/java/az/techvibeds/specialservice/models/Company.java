package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private Boolean active = true;

    @Column(precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(precision = 19, scale = 2)
    private BigDecimal usedBudget;

    @OneToMany(mappedBy = "company")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<CompanyModule> companyModules = new ArrayList<>();


    private Date expressDate;

    @OneToMany(mappedBy = "company")
    private List<CompanyStock> companyStocks = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<ProjectContractor> projectContractorList = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<ManufacturedProduct> products = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<SemiProduct> semiProducts = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<ProductionOrder> productionOrders = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<IndirectExpense> indirectExpenses = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<HREmployee> hrEmployees = new ArrayList<>();

}
