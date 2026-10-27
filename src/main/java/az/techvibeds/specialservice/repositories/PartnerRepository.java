package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.PartnerType;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    List<Partner> findAllByCompanyId(Long id);

    List<Partner> findAllByCompany(Company company);

    List<Partner> findAllByCompanyIdAndPartnerType(Long companyId, PartnerType partnerType);

    List<Partner> findAllByCompanyAndIsActive(Company company, Boolean isActive);
    List<Partner> findAllByPartnerType(PartnerType partnerType);
    long countByCompanyId(Long companyId);

}
