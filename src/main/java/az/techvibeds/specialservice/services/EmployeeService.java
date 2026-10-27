package az.techvibeds.specialservice.services;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface EmployeeService {
    String uploadProfileImage(Long employeeId, MultipartFile file) throws IOException;
}
