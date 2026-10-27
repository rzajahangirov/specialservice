package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestCreateDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestResponseDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestUpdateDto;
import az.techvibeds.specialservice.enums.ActivityStatus;
import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.enums.DeductionType;
import az.techvibeds.specialservice.enums.LeaveType;
import az.techvibeds.specialservice.exceptions.EmployeeNotFoundException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.LeaveRequestService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestServiceImpl implements LeaveRequestService {
    private final LeaveRequestRepository leaveRepository;
    private final HREmployeeRepository employeeRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final ContractRepository contractRepository;
    private final DeductionRepository deductionRepository;
    private final HRActivityRepository hRActivityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EnumResponseDto> getLeaveTypes() {
        log.debug("Fetching leave types for dropdown");
        return Arrays.stream(LeaveType.values())
                .map(type -> EnumResponseDto.builder()
                        .key(type.name())
                        .value(formatEnumValue(type.name()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto createRequest(LeaveRequestCreateDto request, String callerEmail) {
        log.info("Creating leave request for employeeId={} by caller={}", request.getEmployeeId(), callerEmail);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }

        HREmployee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(request.getEmployeeId()));

        validateCompanyAccess(callerEmail, employee);

        LeaveRequest leaveRequest = modelMapper.map(request, LeaveRequest.class);
        leaveRequest.setEmployee(employee);
        leaveRequest.setStatus(ActivityStatus.PENDING);

        LeaveRequest saved = leaveRepository.save(leaveRequest);
        log.info("Leave request created successfully with id={}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto updateRequest(Long id, LeaveRequestUpdateDto request, String callerEmail) {
        log.info("Update attempt for leaveRequestId={} by user={}", id, callerEmail);

        LeaveRequest leaveRequest = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        validateCompanyAccess(callerEmail, leaveRequest.getEmployee());

        if (leaveRequest.getStatus() != ActivityStatus.PENDING) {
            log.warn("Unauthorized update attempt. LeaveRequest id={} is already {}", id, leaveRequest.getStatus());
            throw new IllegalStateException("Cannot update a leave request that is already processed (Approved/Rejected).");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }

        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setLeaveType(request.getLeaveType());
        leaveRequest.setReason(request.getReason());

        LeaveRequest updated = leaveRepository.save(leaveRequest);
        log.info("LeaveRequest id={} successfully updated", id);

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnumResponseDto> getLeaveStatuses() {
        log.debug("Fetching leave request statuses for HR dropdown");
        return Arrays.stream(ActivityStatus.values())
                .map(status -> EnumResponseDto.builder()
                        .key(status.name())
                        .value(formatEnumValue(status.name()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto updateLeaveStatus(Long id, ActivityStatus status, String callerEmail) {
        log.info("Status update attempt: leaveRequestId={}, newStatus={} by user={}", id, status, callerEmail);

        LeaveRequest leaveRequest = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        validateCompanyAccess(callerEmail, leaveRequest.getEmployee());

        if (leaveRequest.getStatus() == status) {
            return mapToResponse(leaveRequest);
        }

        if (status == ActivityStatus.COMPLETED && leaveRequest.getLeaveType() == LeaveType.UNPAID) {
            handleUnpaidLeaveDeduction(leaveRequest);
        }

        leaveRequest.setStatus(status);
        LeaveRequest updated = leaveRepository.save(leaveRequest);

        createAuditActivity(updated, callerEmail);

        log.info("LeaveRequest id={} successfully marked as {}", id, status);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getAllCompanyLeaveRequests(
            ActivityStatus status,
            LeaveType type,
            String callerEmail) {

        log.info("Fetching filtered leave requests for HR: {}, status={}, type={}", callerEmail, status, type);

        User caller = userService.findByEmail(callerEmail);
        List<LeaveRequest> leaveRequests;

        if (caller.getCompany() == null) {
            // Super Admin
            leaveRequests = leaveRepository.findAll().stream()
                    .filter(lr -> (status == null || lr.getStatus() == status) && (type == null || lr.getLeaveType() == type))
                    .collect(Collectors.toList());
        } else {
            leaveRequests = leaveRepository.findAllByFilters(caller.getCompany().getId(), status, type);
        }

        return leaveRequests.stream()
                .map(this::mapToResponse)
                .toList();
    }


    private void handleUnpaidLeaveDeduction(LeaveRequest leave) {
        log.info("Generating automatic deduction for unpaid leave. RequestId={}", leave.getId());

        long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;


        Contract contract = contractRepository.findByEmployeeIdAndIsActiveTrue(leave.getEmployee().getId())
                .orElseThrow(() -> new IllegalStateException("No active contract found for salary calculation"));

        BigDecimal dailySalary = contract.getSalary().divide(new BigDecimal("30"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDeduction = dailySalary.multiply(new BigDecimal(days));

        Deduction unpaidDeduction = new Deduction();
        unpaidDeduction.setEmployee(leave.getEmployee());
        unpaidDeduction.setType(DeductionType.UNPAID_LEAVE);
        unpaidDeduction.setAmount(totalDeduction);
        unpaidDeduction.setReason("Automatic deduction for Unpaid Leave: " + leave.getStartDate() + " to " + leave.getEndDate());
        unpaidDeduction.setStartDate(leave.getStartDate());
        unpaidDeduction.setDuration(1);
        unpaidDeduction.setDurationType(DeductionDurationType.MONTHS);
        unpaidDeduction.setActive(true);
        unpaidDeduction.setDeleted(false);

        deductionRepository.save(unpaidDeduction);
        log.info("Deduction of {} created for employeeId={} due to unpaid leave", totalDeduction, leave.getEmployee().getId());
    }


    private void validateCompanyAccess(String email, HREmployee target) {
        User caller = userService.findByEmail(email);
        if (caller.getCompany() == null) {
            // Super Admin
            return;
        }
        if (!caller.getCompany().getId().equals(target.getCompany().getId())) {
            log.warn("Unauthorized leave request attempt. Caller: {}, Target Company: {}", email, target.getCompany().getId());
            throw new AccessDeniedException("Access Denied: Employee is not in your company.");
        }
    }

    private LeaveRequestResponseDto mapToResponse(LeaveRequest leaveRequest) {
        LeaveRequestResponseDto dto = modelMapper.map(leaveRequest, LeaveRequestResponseDto.class);
        dto.setEmployeeId(leaveRequest.getEmployee().getId());
        dto.setEmployeeFullName(leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName());
        return dto;
    }

    private String formatEnumValue(String name) {
        String lower = name.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    private void createAuditActivity(LeaveRequest leave, String hrEmail) {
        HRActivity activity = new HRActivity();
        activity.setEmployee(leave.getEmployee());

        String description = String.format("Leave Request (%s) marked as %s by HR: %s. Period: %s to %s",
                leave.getLeaveType(),
                leave.getStatus(),
                hrEmail,
                leave.getStartDate(),
                leave.getEndDate());

        activity.setActivityType(description);
        activity.setStatus(leave.getStatus());

        hRActivityRepository.save(activity);
    }
}
