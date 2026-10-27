package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ModuleRole;
import az.techvibeds.specialservice.models.UserModuleRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserModuleRoleRepository extends JpaRepository<UserModuleRole, Long> {
    List<UserModuleRole> findByUserId(Long userId);

    Optional<UserModuleRole> findByUserIdAndCompanyModuleId(Long userId, Long companyModuleId);

    @Query("SELECT umr FROM UserModuleRole umr " +
            "JOIN FETCH umr.companyModule cm " +
            "JOIN FETCH cm.module " +
            "WHERE umr.user.id = :userId")
    List<UserModuleRole> findAllByUserIdWithDetails(@Param("userId") Long userId);

    boolean existsByUserIdAndCompanyModuleId(Long userId, Long companyModuleId);

    long countByCompanyModuleModuleIdAndRole(Long moduleId, ModuleRole role);

    @Modifying
    @Query("DELETE FROM UserModuleRole umr WHERE umr.user.id = :userId")
    void deleteByUserId(Long userId);

    Integer countByCompanyModuleId(Long id);

    List<UserModuleRole> findByCompanyModuleId(Long companyModuleId);
}
