package az.techvibeds.specialservice.controllers.manufacturing;

import az.techvibeds.specialservice.dtos.manufacturing.ManufacturingManagementDto;
import az.techvibeds.specialservice.models.ManufacturedProduct;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ManufacturedProductService;
import az.techvibeds.specialservice.services.ManufacturingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manufacturing")
@Slf4j
public class ManufacturingManagementController {

    private final ManufacturingService manufacturingService;
    private final ManufacturedProductService manufacturedProductService;

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<ManufacturingManagementDto>> getManufacturingManagement(Principal principal) {
        log.info("API request: Get manufacturing management page for user {}", principal.getName());

        ManufacturingManagementDto data = manufacturingService.getDatasForManufacturingPage(principal.getName());

        String message = (data.getInProductionOrderCount() == 0 && data.getMonthlyProductionCount() == 0)
                ? "No production data found for the current period"
                : "Manufacturing data retrieved successfully";

        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }

    // Məhsulları Excel-ə export
    @GetMapping("/products/export")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<byte[]> exportProductsToExcel(Principal principal) throws Exception {
        ByteArrayInputStream in = manufacturedProductService.exportToExcel(principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Mehsullar_" + java.time.LocalDate.now() + ".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    // Excel-dən məhsul idxalı
    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ApiResponse> importProductsFromExcel(@RequestParam("file") MultipartFile file, Principal principal) throws Exception {
        manufacturedProductService.importFromExcel(file, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Məhsullar Excel-dən uğurla idxal edildi!", true));
    }

    /** Expose ManufacturedProduct creation via REST endpoint */
    @PostMapping("/products")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<java.util.Map<String, Object>>> createProduct(
            @jakarta.validation.Valid @RequestBody az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductCreateDto dto,
            Principal principal) {
        ManufacturedProduct data = manufacturedProductService.createProduct(dto, principal.getName());
        java.util.Map<String, Object> response = java.util.Map.of(
            "id", data.getId(),
            "name", data.getName(),
            "description", data.getDescription() != null ? data.getDescription() : ""
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(response, "Manufactured product created successfully"));
    }
}

