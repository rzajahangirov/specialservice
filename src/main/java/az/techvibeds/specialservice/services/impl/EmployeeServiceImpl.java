package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Employee;
import az.techvibeds.specialservice.repositories.EmployeeRepository;
import az.techvibeds.specialservice.services.EmployeeService;
import az.techvibeds.specialservice.services.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final FileService fileService;

    @Value("${project.image.path:uploads/employees}")
    private String path;

    @Override
    public String uploadProfileImage(Long employeeId, MultipartFile file) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found", HttpStatus.NOT_FOUND));

        // Köhnə şəkli varsa silək (isteğe bağlı)
        if (employee.getImageUrl() != null) {
            fileService.deleteFile(path, employee.getImageUrl());
        }

        // Yeni şəkli yükləyək
        String fileName = fileService.uploadImage(path, file);

        // Bazada adı yeniləyək
        employee.setImageUrl(fileName);
        employeeRepository.save(employee);

        return fileName;
    }
}
