package az.techvibeds.specialservice.controllers.inventory;

import az.techvibeds.specialservice.dtos.warehouse.WarehouseCreateDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseReadDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseUpdateDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<WarehouseReadDto> create(@RequestBody WarehouseCreateDto dto, Principal principal) {
        WarehouseReadDto created = warehouseService.create(dto,principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<WarehouseReadDto> update(@RequestBody WarehouseUpdateDto dto, Principal principal) {
        WarehouseReadDto updated = warehouseService.update(dto, principal.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:INVENTORY:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id, Principal principal) {
        warehouseService.delete(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Warehouse deleted", true));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<WarehouseReadDto> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(warehouseService.getById(id, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<List<WarehouseReadDto>> getAllByCompany(Principal principal) {
        return ResponseEntity.ok(warehouseService.getAllByCompanyReadDto(principal.getName()));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<String> toggleStatus(@PathVariable Long id, Principal principal) {
        warehouseService.toggleWarehouseStatus(id, principal.getName());
        return ResponseEntity.ok("Warehouse status successfully updated");
    }
}
