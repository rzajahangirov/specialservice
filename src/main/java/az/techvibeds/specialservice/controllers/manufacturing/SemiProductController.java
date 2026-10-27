package az.techvibeds.specialservice.controllers.manufacturing;

import az.techvibeds.specialservice.dtos.semiProduct.SemiProductCreateDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductReadDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductUpdateDto;
import az.techvibeds.specialservice.enums.SemiProductStatus;
import az.techvibeds.specialservice.models.SemiProduct;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.repositories.SemiProductRepository;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.services.SemiProductService;
import az.techvibeds.specialservice.services.WarehouseProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/semi-product")
public class SemiProductController {
    private final SemiProductService semiProductService;
    private final WarehouseProductService warehouseProductService;
    private final SemiProductRepository semiProductRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<SemiProductReadDto>> create(
            @Valid @RequestBody SemiProductCreateDto dto,
            Principal principal) {
        SemiProductReadDto data = semiProductService.create(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Semi-product created successfully"));
    }

    /** Malı (SemiProduct) anbara daxil etmək */
    @PostMapping("/warehouse-record")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ApiResponse> addToWarehouse(
            @RequestParam Long semiProductId,
            @RequestParam Long warehouseId,
            @RequestParam Integer quantity,
            Principal principal) {
        try {
            SemiProduct semiProduct = semiProductRepository.findById(semiProductId)
                    .orElseThrow(() -> new ResourceNotFoundException("Semi-product", "id", semiProductId));
            warehouseProductService.createWarehouseSemiProduct(semiProduct, warehouseId, quantity, principal.getName());
            return ResponseEntity.ok(new ApiResponse("Semi-product added to warehouse successfully. Quantity: " + quantity, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<List<SemiProductReadDto>>> getSemiProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SemiProductStatus status,
            Principal principal) {
        List<SemiProductReadDto> data = semiProductService.getSemiProducts(keyword, status, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Semi-products retrieved"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN')")
    public ResponseEntity<ResponseDto<SemiProductReadDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody SemiProductUpdateDto dto,
            Principal principal) {
        SemiProductReadDto data = semiProductService.update(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Semi-product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:MANUFACTURING:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        semiProductService.delete(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Semi-product deleted successfully"));
    }

    // Excel-ə export
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<byte[]> exportToExcel(Principal principal) throws Exception {
        ByteArrayInputStream in = semiProductService.exportToExcel(principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Mallar_" + java.time.LocalDate.now() + ".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    // Excel-dən import
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ApiResponse> importFromExcel(@RequestParam("file") MultipartFile file, Principal principal) throws Exception {
        semiProductService.importFromExcel(file, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Excel uğurla idxal edildi!", true));
    }
}