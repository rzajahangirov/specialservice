package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.StageStatus;
import az.techvibeds.specialservice.models.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long> {
    List<ProjectStage> findAllByProjectId(Long projectId);

    @Query("SELECT ps FROM ProjectStage ps JOIN FETCH ps.project p WHERE p.company.id = :companyId AND ps.status = :status")
    List<ProjectStage> findActiveStagesByCompanyId(@Param("companyId") Long companyId, @Param("status") StageStatus status);
}
