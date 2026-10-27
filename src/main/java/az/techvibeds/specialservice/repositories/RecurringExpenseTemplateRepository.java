package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.RecurringTemplateStatus;
import az.techvibeds.specialservice.models.RecurringExpenseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringExpenseTemplateRepository extends JpaRepository<RecurringExpenseTemplate, Long> {
    List<RecurringExpenseTemplate> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<RecurringExpenseTemplate> findByStatusAndNextExecutionDateLessThanEqual(RecurringTemplateStatus status, LocalDate date);
}
