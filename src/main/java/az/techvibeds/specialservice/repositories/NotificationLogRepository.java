package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findTop5ByCompanyIdOrderBySentAtDesc(Long companyId);
}
