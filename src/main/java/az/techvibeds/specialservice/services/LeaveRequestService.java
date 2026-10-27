package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestCreateDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestResponseDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestUpdateDto;
import az.techvibeds.specialservice.enums.ActivityStatus;
import az.techvibeds.specialservice.enums.LeaveType;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface LeaveRequestService {
    List<EnumResponseDto> getLeaveTypes();

    LeaveRequestResponseDto createRequest(@Valid LeaveRequestCreateDto request, String name);

    LeaveRequestResponseDto updateRequest(Long id, @Valid LeaveRequestUpdateDto request, String name);

    List<EnumResponseDto> getLeaveStatuses();

    LeaveRequestResponseDto updateLeaveStatus(Long id, ActivityStatus status, String name);

    List<LeaveRequestResponseDto> getAllCompanyLeaveRequests(ActivityStatus status, LeaveType type, String name);
}
