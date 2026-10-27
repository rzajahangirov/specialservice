package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.CompanyModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CompanyModuleRepository extends JpaRepository<CompanyModule, Long> {
    List<CompanyModule> findByActiveTrueAndEndDateBefore(Date date);

    List<CompanyModule> findAllByCompanyId(Long companyId);

    List<CompanyModule> findAllByCompanyIdAndActiveTrue(Long id);

    Optional<CompanyModule> findByCompanyIdAndModuleId(Long companyId, Long moduleId);

    @Modifying
    @Query("DELETE FROM CompanyModule cm WHERE cm.company.id = :companyId")
    void deleteAllByCompanyId(@Param("companyId") Long companyId);

    /**
     * Şirkətin aktiv modullarının backendKey set-ini qaytarır.
     * Tam entity yükləmədən yalnız key-ləri çəkir — performans üçün.
     */
    @Query("SELECT cm.module.backendKey FROM CompanyModule cm WHERE cm.company.id = :companyId AND cm.active = true AND cm.module IS NOT NULL")
    Set<String> findActiveModuleKeysByCompanyId(@Param("companyId") Long companyId);
}

