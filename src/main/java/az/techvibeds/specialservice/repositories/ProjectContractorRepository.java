package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ProjectContractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectContractorRepository extends JpaRepository<ProjectContractor, Long> {

    @Query("SELECT c FROM ProjectContractor c WHERE c.company.id = :companyId " +
            "AND (:name IS NULL OR :name = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<ProjectContractor> findByCompanyAndName(@Param("companyId") Long companyId, @Param("name") String name);

    List<ProjectContractor> findAllByCompany_Id(Long companyId);
}
