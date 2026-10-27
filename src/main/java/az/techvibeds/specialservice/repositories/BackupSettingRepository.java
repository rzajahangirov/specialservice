package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.BackupSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupSettingRepository extends JpaRepository<BackupSetting, Long> {

    Optional<BackupSetting> findByCompanyId(Long companyId);
}
