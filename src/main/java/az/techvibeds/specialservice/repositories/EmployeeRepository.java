package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAllByCompanyId(Long companyId);
}
