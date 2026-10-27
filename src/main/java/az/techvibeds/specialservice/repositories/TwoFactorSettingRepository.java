package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.TwoFactorSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorSettingRepository extends JpaRepository<TwoFactorSetting, Long> {

    Optional<TwoFactorSetting> findByCompanyId(Long companyId);
}
