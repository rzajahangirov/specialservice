package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.hrEmployee.*;
import az.techvibeds.specialservice.enums.EmploymentStatus;
import az.techvibeds.specialservice.exceptions.AlreadyExistsException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.HREmployee;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.repositories.HREmployeeRepository;
import az.techvibeds.specialservice.repositories.LeaveRequestRepository;
import az.techvibeds.specialservice.repositories.ContractRepository;
import az.techvibeds.specialservice.models.Contract;
import az.techvibeds.specialservice.enums.ContractType;
import az.techvibeds.specialservice.exceptions.BusinessException;
import java.math.BigDecimal;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.FileService;
import az.techvibeds.specialservice.services.HREmployeeService;
import az.techvibeds.specialservice.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HREmployeeServiceImpl implements HREmployeeService {
    private final HREmployeeRepository employeeRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final FileService fileService;
    private final ContractRepository contractRepository;

    @Value("${project.image.path:uploads/employees}")
    private String path;

    @Override
    public String uploadProfileImage(Long employeeId, MultipartFile file, String email) throws IOException {
        HREmployee employee = getEmployeeIfAuthorized(employeeId, email);

        if (employee.getImageUrl() != null) {
            fileService.deleteFile(path, employee.getImageUrl());
        }

        String fileName = fileService.uploadImage(path, file);
        employee.setImageUrl(fileName);
        employeeRepository.save(employee);

        return fileName;
    }

    @Override
    @Transactional
    public HREmployeeResponseDto createEmployee(HREmployeeCreateDto request, String email) {
        log.info("Starting employee creation process for email: {}", request.getEmail());

        Company company = companyService.findByUserEmail(email);

        if (employeeRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to create employee with existing email: {}", request.getEmail());
            throw new AlreadyExistsException("Employee with this email already exists");
        }

        HREmployee employee = modelMapper.map(request, HREmployee.class);
        employee.setCompany(company);

        HREmployee savedEmployee = employeeRepository.save(employee);
        log.info("Employee successfully created with ID: {}", savedEmployee.getId());

        return mapToResponse(savedEmployee);
    }

    @Override
    public ResponseDto<List<HREmployeeListDto>> getAllEmployees(String email, EmploymentStatus status) {
        Company company = companyService.findByUserEmail(email);
        List<HREmployee> employees;
        
        if (company == null) {
            // Super Admin sees all employees
            employees = (status != null) 
                ? employeeRepository.findAll().stream().filter(e -> e.getStatus() == status).collect(Collectors.toList())
                : employeeRepository.findAll();
        } else {
            employees = employeeRepository.findAllByCompanyIdAndStatus(company.getId(), status);
        }

        List<HREmployeeListDto> dtoList = employees.stream()
                .map(e -> modelMapper.map(e, HREmployeeListDto.class))
                .collect(Collectors.toList());

        return new ResponseDto<>(dtoList, "Employees retrieved successfully");
    }

    @Override
    public ResponseDto<HREmployeeDetailDto> getEmployeeDetail(Long id, String email) {
        HREmployee employee = getEmployeeIfAuthorized(id, email);

        HREmployeeDetailDto detailDTO = modelMapper.map(employee, HREmployeeDetailDto.class);
        return new ResponseDto<>(detailDTO, "Employee details retrieved successfully");
    }

    @Override
    public HREmployee getEmployeeIfAuthorized(Long employeeId, String currentUserEmail) {
        Company company = companyService.findByUserEmail(currentUserEmail);

        HREmployee employee = employeeRepository.findByIdWithDeleted(employeeId)
                .orElseThrow(() -> {
                    log.warn("Employee not found with id: {}", employeeId);
                    return new ResourceNotFoundException("Employee", "id", employeeId);
                });

        if (company != null && !employee.getCompany().getId().equals(company.getId())) {
            log.error("Unauthorized access attempt by user {} to employee {}", currentUserEmail, employeeId);
            throw new AccessDeniedException("You do not have permission to access this employee's data");
        } else if (company == null && !userService.findByEmail(currentUserEmail).getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("You do not have permission to access this employee's data");
        }

        return employee;
    }

    @Override
    @Transactional
    public HREmployeeResponseDto updateEmployee(Long id, HREmployeeUpdateDto updateDTO, String currentUserEmail) {
        log.info("User {} is attempting to update employee ID: {}", currentUserEmail, id);

        HREmployee employee = getEmployeeIfAuthorized(id, currentUserEmail);

        modelMapper.map(updateDTO, employee);

        HREmployee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee ID: {} successfully updated by user: {}", id, currentUserEmail);

        return modelMapper.map(updatedEmployee, HREmployeeResponseDto.class);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, EmploymentStatus newStatus, String currentUserEmail) {
        log.info("User {} is changing status of employee ID: {} to {}", currentUserEmail, id, newStatus);

        HREmployee employee = getEmployeeIfAuthorized(id, currentUserEmail);

        EmploymentStatus oldStatus = employee.getStatus();
        employee.setStatus(newStatus);
        employeeRepository.save(employee);

        log.info("Status changed for Employee ID: {}. Old: {}, New: {}", id, oldStatus, newStatus);
    }

    @Override
    public ResponseDto<List<EnumResponseDto>> getEmploymentStatuses() {
        List<EnumResponseDto> statuses = Arrays.stream(EmploymentStatus.values())
                .map(status -> new EnumResponseDto(
                        status.name(),
                        capitalizeFirstLetter(status.name().replace("_", " ").toLowerCase())
                ))
                .collect(Collectors.toList());

        return new ResponseDto<>(statuses, "Employment statuses retrieved successfully");
    }

    @Override
    @Transactional
    public void softDeleteEmployee(Long employeeId, String callerEmail) {
        log.info("Soft delete request: employeeId={}, callerEmail={}", employeeId, callerEmail);

        HREmployee target = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> {
                    log.warn("Employee not found or already deleted: id={}", employeeId);
                    return new ResourceNotFoundException("Employee", "id", employeeId);
                });

        User caller = userService.findByEmail(callerEmail);


        validateSameCompany(caller, target);

        target.setDeleted(true);
        target.setDeletedAt(LocalDateTime.now());

        employeeRepository.save(target);

        log.info("Employee successfully soft-deleted: employeeId={}, deletedBy={}, deletedAt={}",
                target.getId(), callerEmail, target.getDeletedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeletedEmployeeResponseDto> getDeletedEmployeesByCompany(String callerEmail) {
        log.info("Request for deleted employees: callerEmail={}", callerEmail);

        User caller = userService.findByEmail(callerEmail);

        if (caller.getCompany() == null) {
            log.warn("Caller company is null: email={}", callerEmail);
            throw new AccessDeniedException("Access denied: Company information is missing.");
        }

        Long companyId = caller.getCompany().getId();

        List<HREmployee> deletedEmployees =
                employeeRepository.findDeletedEmployeesByCompanyId(companyId);

        log.info("Total deleted employees found: companyId={}, count={}", companyId, deletedEmployees.size());

        return deletedEmployees.stream()
                .map(employee -> modelMapper.map(employee, DeletedEmployeeResponseDto.class))
                .toList();
    }

    @Override
    @Transactional
    public RestoredEmployeeResponseDto restoreEmployee(Long employeeId, String callerEmail) {
        log.info("Restore request: employeeId={}, callerEmail={}", employeeId, callerEmail);

        HREmployee target = employeeRepository.findDeletedById(employeeId)
                .orElseThrow(() -> {
                    log.warn("Deleted employee not found: id={}", employeeId);
                    return new ResourceNotFoundException("Employee", "id", employeeId);
                });

        User caller = userService.findByEmail(callerEmail);

        validateSameCompany(caller, target);

        target.setDeleted(false);
        target.setDeletedAt(null);

        HREmployee restoredEmployee = employeeRepository.save(target);

        log.info("Employee successfully restored: employeeId={}, restoredBy={}", employeeId, callerEmail);

        return modelMapper.map(restoredEmployee, RestoredEmployeeResponseDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public HRDashboardStatsDto getDashboardStats(String callerEmail) {
        log.info("Fetching HR dashboard stats for user={}", callerEmail);

        User caller = userService.findByEmail(callerEmail);
        if (caller.getCompany() == null) {
             return HRDashboardStatsDto.builder()
                .totalEmployees(employeeRepository.count())
                .activeEmployees(0L) 
                .employeesOnLeave(0L)
                .build();
        }
        Long companyId = caller.getCompany().getId();
        LocalDate today = LocalDate.now();

        long total = employeeRepository.countByCompanyIdAndDeletedFalse(companyId);
        long active = employeeRepository.countByCompanyIdAndDeletedFalseAndStatus(companyId, EmploymentStatus.ACTIVE);
        long onLeave = leaveRequestRepository.countEmployeesOnLeaveToday(companyId, today);

        return HRDashboardStatsDto.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .employeesOnLeave(onLeave)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportEmployeesToExcel(HttpServletResponse response, String callerEmail) {
        log.info("Starting excel export for employees by user={}", callerEmail);

        User caller = userService.findByEmail(callerEmail);
        Long companyId = caller.getCompany().getId();

        List<HREmployee> employees = employeeRepository.findAllByCompanyIdAndDeletedFalse(companyId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Ad", "Soyad", "Email", "Telefon", "Vəzifə", "Status", "İşə Qəbul", "Kontrakt Tipi", "Kontrakt B.", "Kontrakt Bi.", "Maaş", "Sənəd Linki"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (HREmployee emp : employees) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(emp.getFirstName() != null ? emp.getFirstName() : "");
                row.createCell(2).setCellValue(emp.getLastName() != null ? emp.getLastName() : "");
                row.createCell(3).setCellValue(emp.getEmail() != null ? emp.getEmail() : "");
                row.createCell(4).setCellValue("-"); // Telefon sahəsi modeldə yoxdur
                row.createCell(5).setCellValue(emp.getPosition() != null ? emp.getPosition() : "");
                row.createCell(6).setCellValue(emp.getStatus() == null ? "" : emp.getStatus().name());
                row.createCell(7).setCellValue(emp.getHireDate() != null ? emp.getHireDate().toString() : "");

                Contract activeContract = contractRepository.findByEmployeeIdAndIsActiveTrue(emp.getId()).orElse(null);
                if (activeContract != null) {
                    row.createCell(8).setCellValue(activeContract.getContractType() != null ? activeContract.getContractType().name() : "");
                    row.createCell(9).setCellValue(activeContract.getStartDate() != null ? activeContract.getStartDate().toString() : "");
                    row.createCell(10).setCellValue(activeContract.getEndDate() != null ? activeContract.getEndDate().toString() : "");
                    row.createCell(11).setCellValue(activeContract.getSalary() != null ? activeContract.getSalary().toString() : "0");
                    row.createCell(12).setCellValue(activeContract.getDocumentUrl() != null ? activeContract.getDocumentUrl() : "-");
                } else {
                    row.createCell(8).setCellValue("Yoxdur");
                    row.createCell(9).setCellValue("-");
                    row.createCell(10).setCellValue("-");
                    row.createCell(11).setCellValue("0");
                    row.createCell(12).setCellValue("-");
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String fileName = "employees_" + LocalDate.now() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            workbook.write(response.getOutputStream());
            log.info("Excel export completed successfully for companyId={}", companyId);

        } catch (IOException e) {
            log.error("Excel export failed", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private void validateSameCompany(User caller, HREmployee target) {
        boolean callerHasNoCompany = caller.getCompany() == null;
        boolean targetHasNoCompany = target.getCompany() == null;

        if (callerHasNoCompany || targetHasNoCompany) {
            log.warn("Company information missing — caller companyId={}, target companyId={}",
                    callerHasNoCompany ? "null" : caller.getCompany().getId(),
                    targetHasNoCompany ? "null" : target.getCompany().getId());
            throw new AccessDeniedException("Access denied: Company information is missing.");
        }

        if (!caller.getCompany().getId().equals(target.getCompany().getId())) {
            log.warn("Company mismatch — callerCompanyId={}, targetCompanyId={}",
                    caller.getCompany().getId(), target.getCompany().getId());
            throw new AccessDeniedException(
                    "You do not have permission to delete this employee. Company mismatch detected.");
        }
    }

    private String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) return original;
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    private HREmployeeResponseDto mapToResponse(HREmployee entity) {
        return HREmployeeResponseDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .hireDate(entity.getHireDate())
                .build();
    }

    @Override
    @Transactional
    public void importEmployeesFromExcel(MultipartFile file, String callerEmail) throws Exception {
        log.info("Starting excel import for employees by user={}", callerEmail);
        User caller = userService.findByEmail(callerEmail);
        if (caller.getCompany() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Company information is missing.");
        }
        Company company = caller.getCompany();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                // Email is Column D (cell index 3)
                String email = getCellValueAsString(row.getCell(3));
                if (email == null || email.isBlank()) continue;

                // Ad is Column B (1), Soyad is Column C (2)
                String firstName = getCellValueAsString(row.getCell(1));
                String lastName = getCellValueAsString(row.getCell(2));
                if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                    throw new BusinessException("Sətir " + (row.getRowNum() + 1) + ": Ad və Soyad mütləqdir!");
                }

                // Vəzifə is Column F (5)
                String position = getCellValueAsString(row.getCell(5));

                // Status is Column G (6)
                String statusStr = getCellValueAsString(row.getCell(6));
                EmploymentStatus status = parseEmploymentStatus(statusStr);

                // İşə Qəbul is Column H (7)
                LocalDate hireDate = parseLocalDateCell(row.getCell(7));
                if (hireDate == null) {
                    hireDate = LocalDate.now();
                }

                // Find existing employee by email and company (active/not deleted)
                HREmployee employee = employeeRepository.findActiveByEmail(email).orElse(null);
                
                boolean isNew = false;
                if (employee == null) {
                    employee = new HREmployee();
                    employee.setEmail(email);
                    employee.setCompany(company);
                    isNew = true;
                }

                // Update employee info
                employee.setFirstName(firstName);
                employee.setLastName(lastName);
                employee.setPosition(position);
                employee.setStatus(status);
                employee.setHireDate(hireDate);

                HREmployee savedEmployee = employeeRepository.save(employee);

                // Kontrakt Tipi is Column I (8)
                String contractTypeStr = getCellValueAsString(row.getCell(8));
                if (contractTypeStr != null && !contractTypeStr.isBlank() && !contractTypeStr.equalsIgnoreCase("Yoxdur") && !contractTypeStr.equals("-")) {
                    ContractType contractType = parseContractType(contractTypeStr);
                    
                    // J: StartDate, K: EndDate, L: Salary, M: DocumentUrl
                    LocalDate startDate = parseLocalDateCell(row.getCell(9));
                    LocalDate endDate = parseLocalDateCell(row.getCell(10));
                    BigDecimal salary = getCellValueAsBigDecimal(row.getCell(11));
                    String docUrl = getCellValueAsString(row.getCell(12));

                    if (startDate == null) {
                        startDate = hireDate;
                    }

                    // Check if employee already has an active contract
                    Contract contract = null;
                    if (!isNew) {
                        contract = contractRepository.findByEmployeeIdAndIsActiveTrue(savedEmployee.getId()).orElse(null);
                    }

                    if (contract == null) {
                        contract = new Contract();
                        contract.setEmployee(savedEmployee);
                        contract.setActive(true);
                    }

                    contract.setContractType(contractType);
                    contract.setStartDate(startDate);
                    contract.setEndDate(endDate);
                    contract.setSalary(salary != null ? salary : BigDecimal.ZERO);
                    contract.setDocumentUrl(docUrl != null && !docUrl.equals("-") && !docUrl.isBlank() ? docUrl : null);

                    contractRepository.save(contract);
                }
            }
            log.info("Excel import completed successfully for companyId={}", company.getId());
        }
    }

    private EmploymentStatus parseEmploymentStatus(String val) {
        if (val == null || val.isBlank()) return EmploymentStatus.ACTIVE;
        val = val.trim().toUpperCase();
        if (val.contains("ACTIVE") || val.contains("AKTIV") || val.contains("İŞLƏYİR") || val.contains("ISLEYIR")) {
            return EmploymentStatus.ACTIVE;
        }
        if (val.contains("INACTIVE") || val.contains("DEAKTIV") || val.contains("PASIV") || val.contains("İŞLƏMİR") || val.contains("ISLEMIR")) {
            return EmploymentStatus.INACTIVE;
        }
        if (val.contains("LEAVE") || val.contains("MEZUNIYYET") || val.contains("MƏZUNİYYƏT")) {
            return EmploymentStatus.ON_LEAVE;
        }
        try {
            return EmploymentStatus.valueOf(val);
        } catch (Exception e) {
            return EmploymentStatus.ACTIVE;
        }
    }

    private ContractType parseContractType(String val) {
        if (val == null || val.isBlank()) return ContractType.FULL_TIME;
        val = val.trim().toUpperCase();
        if (val.contains("FULL") || val.contains("TAM") || val.contains("ŞTAT") || val.contains("STAT")) {
            return ContractType.FULL_TIME;
        }
        if (val.contains("PART") || val.contains("YARIM")) {
            return ContractType.PART_TIME;
        }
        if (val.contains("FREE") || val.contains("XİDMƏT") || val.contains("XIDMET") || val.contains("FİRM")) {
            return ContractType.FREELANCE;
        }
        if (val.contains("INTERN") || val.contains("TECRUBE") || val.contains("TƏCRÜBƏ")) {
            return ContractType.INTERNSHIP;
        }
        try {
            return ContractType.valueOf(val);
        } catch (Exception e) {
            return ContractType.FULL_TIME;
        }
    }

    private LocalDate parseLocalDateCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else {
                return LocalDate.ofEpochDay((long) cell.getNumericCellValue());
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty() || val.equals("-")) return null;
            try {
                return LocalDate.parse(val);
            } catch (Exception e) {
                try {
                    String[] parts = val.split("\\.");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        return LocalDate.of(year, month, day);
                    }
                } catch (Exception ex) {
                    try {
                        String[] partsSlash = val.split("/");
                        if (partsSlash.length == 3) {
                            int day = Integer.parseInt(partsSlash[0]);
                            int month = Integer.parseInt(partsSlash[1]);
                            int year = Integer.parseInt(partsSlash[2]);
                            return LocalDate.of(year, month, day);
                        }
                    } catch (Exception ex2) {
                        // ignore and return null
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty() || val.equals("-")) return null;
            try {
                return new BigDecimal(val);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }
}

