package az.techvibeds.specialservice.controllers.inventory;


import az.techvibeds.specialservice.dtos.inventory.InventoryStatisticsDto;
import az.techvibeds.specialservice.dtos.inventory.ProductStatusDto;
import az.techvibeds.specialservice.dtos.product.ProductCreateDto;
import az.techvibeds.specialservice.dtos.product.ProductInventoryDto;
import az.techvibeds.specialservice.dtos.product.ProductInventoryUpdateDto;
import az.techvibeds.specialservice.dtos.barcode.BarcodeSearchResultDto;
import az.techvibeds.specialservice.dtos.product.ProductReadDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.*;
import az.techvibeds.specialservice.services.impl.BarcodeImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final ProductService productService;
    private final CompanyService companyService;
    private final WarehouseProductService warehouseProductService;
    private final InventoryService inventoryService;
    private final BarcodeService barcodeService;
    private final BarcodeImageService barcodeImageService;

    //Melumatlarin getirilmesi
    @GetMapping("statistics")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<InventoryStatisticsDto> inventory(Principal principal) {
        InventoryStatisticsDto inventoryDto = inventoryService.getInventory(principal.getName());
        return ResponseEntity.ok(inventoryDto);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProductInventoryDto>>> getAllProducts(
            @RequestParam(required = false) az.techvibeds.specialservice.enums.ProductType productType,
            Principal principal) {
        List<ProductInventoryDto> readDtoList = productService.getProductsByCompany(principal.getName(), productType);
        ResponseDto<List<ProductInventoryDto>> responseDto = new ResponseDto<>();
        responseDto.setData(readDtoList);
        if (readDtoList.isEmpty()) {
            responseDto.setMessage("No products found");
        }
        responseDto.setMessage("Success");
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }


    //Excelden import
    @PostMapping(value = "/upload-products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ApiResponse> uploadExcel(@RequestParam("file") MultipartFile file, Principal principal) throws Exception {
        productService.uploadProductsFromExcel(file,companyService.findByUserEmail(principal.getName()));
        return ResponseEntity.ok(new ApiResponse("Excel successfully uploaded and saved to DB!" ,true));
    }

    //Excel-e export
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<byte[]> exportToExcel(Principal principal) throws Exception {
        ByteArrayInputStream in = productService.exportProductsToExcel(principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Inventar_" +
                java.time.LocalDate.now() + ".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    //Anbara qeydiyyat
    @PostMapping("/record")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ProductReadDto> inventoryRecord(@Valid @RequestBody ProductCreateDto productCreateDto, Principal principal) throws Exception {
        ProductReadDto created = productService.createProductAndInventoryRecord(productCreateDto,companyService.findByUserEmail(principal.getName()));
        return ResponseEntity.ok(created);
    }

    //Anbara miqdar əlavə etmə
    @PostMapping("/add-quantity")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ApiResponse> addQuantity(@RequestParam String productCode,
                                                   @RequestParam Long warehouseId,
                                                   @RequestParam @Min(1) Integer quantity,
                                                   Principal principal) {
        productService.addQuantityToProduct(productCode, warehouseId, quantity, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Quantity added successfully.", true));
    }
    //Anbardan cixis
    @PostMapping("/remove")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<ApiResponse> removeFromWarehouse(@RequestParam String productCode, @RequestParam Long warehouseId, @RequestParam Integer quantity, Principal principal) {
        try {
            warehouseProductService.removeFromWarehouse(productCode, warehouseId, quantity, principal.getName());
            return ResponseEntity.ok(new ApiResponse("Product removed from warehouse successfully.", true));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse("Error during removing: " + e.getMessage(), false));
        }
    }

    //transfer
    @PostMapping("/warehouse-transfer")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<ApiResponse> transferProduct(@RequestParam String productCode, @RequestParam Long fromWarehouseId, @RequestParam Long toWarehouseId, @RequestParam int quantity, Principal principal) {
        try {
            warehouseProductService.transferProductBetweenWarehouses(productCode, fromWarehouseId, toWarehouseId, quantity, principal.getName());
            return ResponseEntity.ok(new ApiResponse("Product successfully transferred from " + fromWarehouseId + " to " + toWarehouseId, true));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse("Error during transfer: " + e.getMessage(), false));
        }
    }

    //update inventory(warehouseProduct vasiesi ile hem product hemde warehouseProductUpdate olunur)
    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<ProductReadDto> updateInventory(@RequestBody ProductInventoryUpdateDto productInventoryUpdateDto, Principal principal) throws Exception {
        ProductReadDto dto = productService.updateInventorProduct(productInventoryUpdateDto, principal.getName());
        return ResponseEntity.ok(dto);
    }


    //delete inventory
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:INVENTORY:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> deleteInventory(@PathVariable Long id, Principal principal) {
        warehouseProductService.deleteWarehouseProduct(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Product deleted successfully.", true));
    }

    @GetMapping("/get-status")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ProductStatusDto> getInventoryStatus() {
        ProductStatusDto productStatusDto = inventoryService.getProductStatuses();
        return ResponseEntity.ok(productStatusDto);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<List<ProductInventoryDto>> filterProducts( @RequestParam(required = false) Long warehouseId,
                                                                     @RequestParam(required = false) Long categoryId,
                                                                     @RequestParam(required = false) String productStatus,
                                                                     Principal principal){
        List<ProductInventoryDto> productInventoryDtoList = productService
                .getFilteredProducts(warehouseId, categoryId, productStatus, principal.getName());
        return ResponseEntity.ok(productInventoryDtoList);
    }
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<List<ProductInventoryDto>> searchProducts(@RequestParam String productName, Principal principal) {
        List<ProductInventoryDto> productInventoryDtoList = productService.getSearchedProducts(productName, principal);
        return ResponseEntity.ok(productInventoryDtoList);
    }

    @GetMapping("/barcode")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<BarcodeSearchResultDto> getProductByBarcode(@RequestParam String barcode, Principal principal) {
        BarcodeSearchResultDto result = barcodeService.searchByBarcode(barcode, principal.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Məhsulun barkod şəklini (PNG formatında) qaytarır.
     * Stiker çap etmək üçün istifadə olunur.
     * GET /api/v1/inventory/barcode/image?barcode=2000106789014
     */
    @GetMapping("/barcode/image")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<byte[]> getBarcodeImage(@RequestParam String barcode, Principal principal) throws Exception {
        BarcodeSearchResultDto result = barcodeService.searchByBarcode(barcode, principal.getName());
        java.math.BigDecimal price = result.getPriceWithVat() != null ? result.getPriceWithVat() : result.getPrice();
        byte[] image = barcodeImageService.generateBarcodeImage(barcode, result.getName(), price);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.add("Content-Disposition", "inline; filename=barcode_" + barcode + ".png");
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    /**
     * Sürətli barkod skan ilə anbara əlavə etmə.
     * - Məhsul varsa: stokunu 1 artırır
     * - Yoxdursa: xarici bazadan adını tapıb avtomatik yaradır
     * POST /api/v1/inventory/quick-scan?barcode=...&warehouseId=1
     */
    @PostMapping("/quick-scan")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<ProductReadDto> quickScan(
            @RequestParam String barcode,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        ProductReadDto result = productService.quickScanToInventory(barcode, warehouseId, categoryId, email);
        return ResponseEntity.ok(result);
    }

}
