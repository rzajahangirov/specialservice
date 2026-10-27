package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE a.actorEmail IN :emails ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentByActorEmails(@Param("emails") List<String> emails, Pageable pageable);
}
