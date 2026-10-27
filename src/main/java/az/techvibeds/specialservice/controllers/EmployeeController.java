package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.EmployeeService;
import az.techvibeds.specialservice.services.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FileService fileService;

    @Value("${project.image.path:uploads/employees}")
    private String path;

    // Şəkli yükləmək üçün endpoint
    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse> uploadEmployeeImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) throws IOException {
        
        String fileName = employeeService.uploadProfileImage(id, image);
        return new ResponseEntity<>(new ApiResponse("Image uploaded successfully: " + fileName, true), HttpStatus.OK);
    }

    // Şəkli brauzerdə görmək (serve etmək) üçün endpoint
    @GetMapping(value = "/image/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public void downloadImage(
            @PathVariable("imageName") String imageName,
            HttpServletResponse response) throws IOException {
        
        InputStream resource = fileService.getResource(path, imageName);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        StreamUtils.copy(resource, response.getOutputStream());
    }
}
