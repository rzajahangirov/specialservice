package az.techvibeds.specialservice.controllers.partner;

import az.techvibeds.specialservice.dtos.partner.*;
import az.techvibeds.specialservice.enums.PartnerType;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.PartnerService;
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
import java.util.Map;

/**
 * Partner (Müştəri & Təchizatçı) Controller.
 * Satış & Alış İdarəetmənin "Müştərilər və Təchizatçılar" tab-ı üçün.
 *
 * Bu controller TransactionController-dəki partner endpoint-lərinin
 * yeni API v1 standartına uyğun versiyasıdır.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/partners")
public class PartnerController {

    private final PartnerService partnerService;

    // ═══════════════════════════════════════════════════════════
    //  YARATMA
    // ═══════════════════════════════════════════════════════════

    /**
     * Yeni müştəri yaradır.
     * POST /api/v1/partners/customer
     */
    @PostMapping("/customer")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerResponseDto> createCustomer(
            @Valid @RequestBody PartnerCreateDto dto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        PartnerResponseDto created = partnerService.createCustomer(dto, email);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Yeni təchizatçı yaradır.
     * POST /api/v1/partners/supplier
     */
    @PostMapping("/supplier")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerResponseDto> createSupplier(
            @Valid @RequestBody PartnerCreateDto dto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        PartnerResponseDto created = partnerService.createSupplier(dto, email);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ═══════════════════════════════════════════════════════════
    //  OXUMA & FİLTR
    // ═══════════════════════════════════════════════════════════

    /**
     * Partnerləri siyahılayır (aktiv statusuna görə filtr).
     * GET /api/v1/partners?active=true
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<?> getPartners(
            @RequestParam(required = false) Boolean active,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            List<PartnerReadDto> partners = partnerService.getPartnersByActiveStatus(active, email);
            return ResponseEntity.ok(partners);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Növə görə filtr (CUSTOMER / SUPPLIER).
     * GET /api/v1/partners/filter?type=CUSTOMER
     */
    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<List<PartnerReadDto>> filterByType(
            @RequestParam("type") PartnerType partnerType,
            Principal principal) throws Exception {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        List<PartnerReadDto> partners = partnerService.findByPartnerTypeAndCompany_Id(partnerType, email);
        return ResponseEntity.ok(partners);
    }

    /**
     * Partner növlərini gətirir (dropdown üçün).
     * GET /api/v1/partners/types
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerTypeDto> getPartnerTypes() {
        PartnerTypeDto types = partnerService.getPartnerTypes();
        return ResponseEntity.ok(types);
    }

    // ═══════════════════════════════════════════════════════════
    //  YENİLƏMƏ
    // ═══════════════════════════════════════════════════════════

    /**
     * Partner məlumatlarını yeniləyir.
     * PUT /api/v1/partners
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN')")
    public ResponseEntity<PartnerResponseDto> updatePartner(
            @Valid @RequestBody PartnerUpdateDto dto,
            Principal principal) throws Exception {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        PartnerResponseDto updated = partnerService.updatePartner(dto, email);
        return ResponseEntity.ok(updated);
    }

    /**
     * Partnerin aktiv/passiv statusunu dəyişir.
     * PATCH /api/v1/partners/{id}/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN')")
    public ResponseEntity<?> toggleActive(
            @PathVariable Long id,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            partnerService.toggleActive(id, email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Partner statusu dəyişdirildi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SİLMƏ
    // ═══════════════════════════════════════════════════════════

    /**
     * Partneri silir.
     * DELETE /api/v1/partners/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:PARTNER:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> deletePartner(
            @PathVariable Long id,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        partnerService.delete(id, email);
        return ResponseEntity.ok(new ApiResponse("Partner silindi", true));
    }

    // ═══════════════════════════════════════════════════════════
    //  EXCEL İDXAL / İXRAC
    // ═══════════════════════════════════════════════════════════

    /**
     * Excel-dən partnerləri idxal edir.
     * POST /api/v1/partners/import
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ApiResponse> importFromExcel(
            @RequestParam("file") MultipartFile file,
            Principal principal) throws Exception {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        partnerService.importFromExcel(file, email);
        return ResponseEntity.ok(new ApiResponse("Excel uğurla idxal edildi!", true));
    }

    /**
     * Partnerləri Excel-ə export edir.
     * GET /api/v1/partners/export
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<byte[]> exportToExcel(Principal principal) throws Exception {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        ByteArrayInputStream in = partnerService.exportToExcel(email);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=partners.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in.readAllBytes());
    }
}
