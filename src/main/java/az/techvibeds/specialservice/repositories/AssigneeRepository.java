package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Assignee;
import az.techvibeds.specialservice.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssigneeRepository extends JpaRepository<Assignee, Long> {
    List<Assignee> findAllByCompany(Company company);
}
