package az.techvibeds.specialservice.controllers.auth;

import az.techvibeds.specialservice.dtos.role.RoleRequestDto;
import az.techvibeds.specialservice.dtos.role.RoleResponseDto;
import az.techvibeds.specialservice.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<RoleResponseDto> create(@Valid @RequestBody RoleRequestDto dto) {
        return new ResponseEntity<>(roleService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<RoleResponseDto> update(@PathVariable Long id, @Valid @RequestBody RoleRequestDto dto) {
        return ResponseEntity.ok(roleService.update(id, dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<RoleResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<List<RoleResponseDto>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
