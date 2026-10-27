package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.RoleStatus;
import az.techvibeds.specialservice.models.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, Long> {

    List<RoleTemplate> findAllByCompanyId(Long companyId);

    List<RoleTemplate> findAllByCompanyIdAndStatus(Long companyId, RoleStatus status);

    Optional<RoleTemplate> findByNameAndCompanyId(String name, Long companyId);

    boolean existsByNameAndCompanyId(String name, Long companyId);

    int countByCompanyId(Long companyId);
}
