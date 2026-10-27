package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.HRActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HRActivityRepository extends JpaRepository<HRActivity, Long> {
    List<HRActivity> findAllByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<HRActivity> findAllByEmployeeCompanyIdOrderByCreatedAtDesc(Long companyId);
}
