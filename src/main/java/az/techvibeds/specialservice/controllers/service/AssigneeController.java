package az.techvibeds.specialservice.controllers.service;

import az.techvibeds.specialservice.dtos.assignee.*;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.AssigneeService;
import az.techvibeds.specialservice.services.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assignee")
public class AssigneeController {

    private final AssigneeService assigneeService;
    private final FileService fileService;

    @Value("${project.image.path:uploads/assignees}")
    private String path;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<AssigneeReadDto> create(
            @Valid @RequestBody AssigneeCreateDto dto,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assigneeService.create(dto, principal.getName()));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority( 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN')")
    public ResponseEntity<AssigneeReadDto> update(
            @Valid @RequestBody AssigneeUpdateDto dto,
            Principal principal) {
        return ResponseEntity.ok(assigneeService.update(dto, principal.getName()));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<AssigneeReadDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image,
            Principal principal) throws IOException {
        return ResponseEntity.ok(assigneeService.uploadImage(id, image, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:SERVICE:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id, Principal principal){
        assigneeService.delete(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Deleted", true));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<AssigneeDetailDto> getAssigneeDetails(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(assigneeService.findAssigneeByIdDetailDto(id, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<ResponseDto<List<AssigneeServiceDto>>> getAllByCompany(Principal principal) {
        List<AssigneeServiceDto> list = assigneeService.getAssigneeByCompany(principal.getName());

        String message = list.isEmpty() ? "No assignee found" : "All assignees found";

        ResponseDto<List<AssigneeServiceDto>> response = new ResponseDto<>();
        response.setData(list);
        response.setMessage(message);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/image/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public void downloadImage(
            @PathVariable("imageName") String imageName,
            HttpServletResponse response) throws IOException {

        InputStream resource = fileService.getResource(path, imageName);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        StreamUtils.copy(resource, response.getOutputStream());
    }
}
