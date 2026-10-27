package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.hrEmployee.*;
import az.techvibeds.specialservice.enums.EmploymentStatus;
import az.techvibeds.specialservice.models.HREmployee;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface HREmployeeService {
    HREmployeeResponseDto createEmployee(@Valid HREmployeeCreateDto request, String email);

    ResponseDto<List<HREmployeeListDto>> getAllEmployees(String name, EmploymentStatus status);

    ResponseDto<HREmployeeDetailDto> getEmployeeDetail(Long id, String name);

    HREmployee getEmployeeIfAuthorized(Long employeeId, String email);

    HREmployeeResponseDto updateEmployee(Long id, @Valid HREmployeeUpdateDto updateDTO, String name);

    void updateStatus(Long id, @NotNull(message = "Status is mandatory") EmploymentStatus status, String name);

    ResponseDto<List<EnumResponseDto>> getEmploymentStatuses();

    void softDeleteEmployee(Long id, String name);

    List<DeletedEmployeeResponseDto> getDeletedEmployeesByCompany(String name);

    RestoredEmployeeResponseDto restoreEmployee(Long id, String name);

    HRDashboardStatsDto getDashboardStats(String name);

    void exportEmployeesToExcel(HttpServletResponse response, String name);
    
    String uploadProfileImage(Long employeeId, org.springframework.web.multipart.MultipartFile file, String email) throws java.io.IOException;

    void importEmployeesFromExcel(org.springframework.web.multipart.MultipartFile file, String name) throws Exception;
}
