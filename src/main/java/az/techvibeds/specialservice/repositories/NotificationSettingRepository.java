package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByCompanyId(Long companyId);
}
