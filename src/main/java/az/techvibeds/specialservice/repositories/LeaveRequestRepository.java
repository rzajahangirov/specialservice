package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ActivityStatus;
import az.techvibeds.specialservice.enums.LeaveType;
import az.techvibeds.specialservice.models.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.company.id = :companyId " +
            "AND (:status IS NULL OR l.status = :status) " +
            "AND (:type IS NULL OR l.leaveType = :type) " +
            "ORDER BY l.startDate DESC")
    List<LeaveRequest> findAllByFilters(
            Long companyId,
            ActivityStatus status,
            LeaveType type);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.employee.company.id = :companyId " +
            "AND l.status = 'COMPLETED' " +
            "AND :today BETWEEN l.startDate AND l.endDate")
    long countEmployeesOnLeaveToday(Long companyId, LocalDate today);
}
