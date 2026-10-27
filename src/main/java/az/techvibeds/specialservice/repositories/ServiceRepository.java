package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.dtos.service.ServiceStatusCount;
import az.techvibeds.specialservice.enums.ServiceStatus;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findAllByCompany(Company company);

    @Query("""
    SELECT s FROM Service s
    WHERE s.company.id = :companyId
      AND (:status IS NULL OR s.status = :status)
      AND (:keyword IS NULL OR LOWER(s.name) LIKE :keyword) 
""")
    List<Service> filterServices(
            @Param("companyId") Long companyId,
            @Param("status") ServiceStatus status,
            @Param("keyword") String keyword
    );
    @Query("SELECT s.status AS status, COUNT(s) AS count FROM Service s WHERE s.company.id = :companyId GROUP BY s.status")
    List<ServiceStatusCount> countServicesByStatusAndCompanyId(@Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);

}
