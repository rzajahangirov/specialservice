package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByCompanyId(Long companyId);

    Page<User> findAllByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE company_id = :companyId AND deleted = true", nativeQuery = true)
    List<User> findAllDeletedByCompanyId(@Param("companyId") Long companyId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET deleted = false, active = true WHERE id = :userId", nativeQuery = true)
    void restoreUserById(@Param("userId") Long userId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.company LEFT JOIN FETCH u.roles")
    List<User> findAllWithCompanyAndRoles();
}
