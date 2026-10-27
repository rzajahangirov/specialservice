package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {

    /** Şirkət və ilə görə ardıcıllıq tapır */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceSequence> findByCompanyAndYear(Company company, Integer year);

    /** Kilidləmədən (read-only) tapmaq üçün */
    Optional<InvoiceSequence> readByCompanyAndYear(Company company, Integer year);
}
