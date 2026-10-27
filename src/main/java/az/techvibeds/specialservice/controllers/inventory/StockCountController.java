package az.techvibeds.specialservice.controllers.inventory;

import az.techvibeds.specialservice.dtos.inventory.StockCountCreateDto;
import az.techvibeds.specialservice.dtos.inventory.StockCountReadDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.StockCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/stock-count")
@RequiredArgsConstructor
@Tag(name = "Stock Count", description = "Inventory Audit (Ehtiyat Sayımı) management APIs")
public class StockCountController {

    private final StockCountService stockCountService;

    @PostMapping
    @Operation(summary = "Create a new stock count audit (DRAFT)")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<StockCountReadDto> createStockCount(@RequestBody StockCountCreateDto dto, Principal principal) {
        return ResponseEntity.ok(stockCountService.createStockCount(dto, principal.getName()));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a draft stock count audit")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<StockCountReadDto> updateStockCount(@PathVariable Long id, @RequestBody StockCountCreateDto dto, Principal principal) {
        return ResponseEntity.ok(stockCountService.updateStockCount(id, dto, principal.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock count details by ID")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<StockCountReadDto> getStockCount(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(stockCountService.getStockCountById(id, principal.getName()));
    }

    @GetMapping
    @Operation(summary = "Get all stock counts for the company")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<List<StockCountReadDto>> getAllStockCounts(Principal principal) {
        return ResponseEntity.ok(stockCountService.getAllStockCounts(principal.getName()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete the stock count and update actual warehouse quantities")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<StockCountReadDto> completeStockCount(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(stockCountService.completeStockCount(id, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft stock count")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<ApiResponse> deleteStockCount(@PathVariable Long id, Principal principal) {
        stockCountService.deleteStockCount(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Stock count deleted successfully", true));
    }
}
