package az.techvibeds.specialservice.controllers.service;

import az.techvibeds.specialservice.dtos.unit.UnitCreateDto;
import az.techvibeds.specialservice.dtos.unit.UnitResponseDto;
import az.techvibeds.specialservice.dtos.unit.UnitUpdateDto;
import az.techvibeds.specialservice.enums.UnitCategory;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/unit")
@Slf4j
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR', 'MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR', 'MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR', 'MODULE:FINANCE:VIEWER', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR', 'MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ResponseDto<List<UnitResponseDto>>> getAllUnits() {
        List<UnitResponseDto> data = unitService.getAllUnits();

        String message = data.isEmpty() ? "No units available" : "Units retrieved successfully";

        ResponseDto<List<UnitResponseDto>> response = new ResponseDto<>(data, message);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR', 'MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR', 'MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR', 'MODULE:FINANCE:VIEWER', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR', 'MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ResponseDto<List<UnitResponseDto>>> getProductUnits() {
        List<UnitResponseDto> data = unitService.getUnitsByCategory(UnitCategory.PRODUCT);
        String message = data.isEmpty() ? "No product units available" : "Product units retrieved successfully";
        ResponseDto<List<UnitResponseDto>> response = new ResponseDto<>(data, message);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/service")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR', 'MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR', 'MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR', 'MODULE:FINANCE:VIEWER', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR', 'MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ResponseDto<List<UnitResponseDto>>> getServiceUnits() {
        List<UnitResponseDto> data = unitService.getUnitsByCategory(UnitCategory.SERVICE);
        String message = data.isEmpty() ? "No service units available" : "Service units retrieved successfully";
        ResponseDto<List<UnitResponseDto>> response = new ResponseDto<>(data, message);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ResponseDto<UnitResponseDto>> create(@Valid @RequestBody UnitCreateDto dto) {
        UnitResponseDto created = unitService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(created, "Unit created successfully"));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN')")
    public ResponseEntity<ResponseDto<UnitResponseDto>> update(@Valid @RequestBody UnitUpdateDto dto) {
        UnitResponseDto updated = unitService.update(dto);
        return ResponseEntity.ok(new ResponseDto<>(updated, "Unit updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PARTNER:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id) {
        unitService.delete(id);
        return ResponseEntity.ok(new ResponseDto<>(null, "Unit deleted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR', 'MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR', 'MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR', 'MODULE:FINANCE:VIEWER', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR', 'MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ResponseDto<UnitResponseDto>> getById(@PathVariable Long id) {
        UnitResponseDto data = unitService.getById(id);
        return ResponseEntity.ok(new ResponseDto<>(data, "Unit found"));
    }
}


