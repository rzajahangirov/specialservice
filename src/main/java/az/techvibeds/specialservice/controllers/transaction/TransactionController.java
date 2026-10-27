package az.techvibeds.specialservice.controllers.transaction;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transaction")
// Satış və Alış İdarəetmə
public class TransactionController {
    private final PartnerService partnerService;

    // Musteri yaratmaq
    @PostMapping("/create-customer")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerResponseDto> createCustomer(@Valid @RequestBody PartnerCreateDto partnerCreateDto, Principal principal) {
        PartnerResponseDto created = partnerService.createCustomer(partnerCreateDto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Techizatci yaratmaq
    @PostMapping("/create-supplier")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerResponseDto> createSupplier(@Valid @RequestBody PartnerCreateDto partnerCreateDto, Principal principal) {
        PartnerResponseDto created = partnerService.createSupplier(partnerCreateDto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

// Musderiler ve Techizatcilar list
//    @GetMapping
//    @PreAuthorize("hasAuthority('MODULE:TRANSACTION:VIEWER')")
//    public ResponseEntity<List<PartnerReadDto>> getPartners(Principal principal) {
//        List<PartnerReadDto> partnerDto = partnerService.getAllPartnersByCompany(principal.getName());
//        return new ResponseEntity<>(partnerDto, HttpStatus.OK);
//    }

    @GetMapping("/partner-types")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<PartnerTypeDto> getPartnerTypes() {
        PartnerTypeDto partnerTypeDtoList = partnerService.getPartnerTypes();
        return ResponseEntity.ok(partnerTypeDtoList);
    }
    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<List<PartnerReadDto>> filterPartners(@RequestParam("filter") PartnerType partnerType, Principal principal) throws Exception {
        List<PartnerReadDto> partnerDtoList = partnerService.findByPartnerTypeAndCompany_Id(partnerType,principal.getName());
        return new ResponseEntity<>(partnerDtoList, HttpStatus.OK);
    }



    // Excel ile idxal
    @PostMapping(
            value = "/upload-partners",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<ApiResponse> uploadExcel(@RequestParam("file") MultipartFile file, Principal principal) throws Exception {
        partnerService.importFromExcel(file, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Excel successfully uploaded and saved to DB!", true));
    }

    //export
    @GetMapping("/export-partners")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<byte[]> exportPartnersToExcel(Principal principal) throws Exception {
        ByteArrayInputStream in = partnerService.exportToExcel(principal.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=partners.xlsx");

        return ResponseEntity.ok().headers(headers).body(in.readAllBytes());
    }


    // Update partner elements
    @PutMapping("/update-partner")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN')")
    public ResponseEntity<PartnerResponseDto> updatePartner(@Valid @RequestBody PartnerUpdateDto partnerUpdateDto, Principal principal) throws Exception {
        PartnerResponseDto partnerReadDto = partnerService.updatePartner(partnerUpdateDto, principal.getName());
        return new ResponseEntity<>(partnerReadDto, HttpStatus.OK);
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN')")
    public ResponseEntity<?> togglePartnerActive(
            @PathVariable Long id,
            Principal principal) {

        try {
            partnerService.toggleActive(id, principal.getName());

            return ResponseEntity.ok().body(
                    Map.of(
                            "success", true,
                            "message", "Partner statusu dəyişdirildi"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/partners")
    @PreAuthorize("hasAnyAuthority('MODULE:PARTNER:VIEWER', 'MODULE:PARTNER:SUPER_ADMIN', 'MODULE:PARTNER:ADMIN', 'MODULE:PARTNER:CREATOR')")
    public ResponseEntity<?> getPartners(
            @RequestParam(required = false) Boolean active,
            Principal principal) {

        try {

            List<PartnerReadDto> partners =
                    partnerService.getPartnersByActiveStatus(active, principal.getName());

            return ResponseEntity.ok(partners);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }



    // Delete partner
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:PARTNER:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> deletePartner(@PathVariable Long id, Principal principal) {
        partnerService.delete(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Partner deleted", true));
    }

}
