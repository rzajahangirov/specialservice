package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {
    Page<PaymentPlan> findAllByCompanyId(Long companyId, Pageable pageable);
}
