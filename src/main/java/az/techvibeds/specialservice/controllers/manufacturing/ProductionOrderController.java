package az.techvibeds.specialservice.controllers.manufacturing;

import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderCreateDto;
import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderReadDto;
import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderUpdateDto;
import az.techvibeds.specialservice.enums.ProductionOrderStatus;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ProductionOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/production-order")
public class ProductionOrderController {
    private final ProductionOrderService productionOrderService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<ProductionOrderReadDto>> create(
            @Valid @RequestBody ProductionOrderCreateDto dto,
            Principal principal) {
        ProductionOrderReadDto data = productionOrderService.createProductionServiceAndProduct(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Order created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProductionOrderReadDto>>> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductionOrderStatus status,
            Principal principal) {
        List<ProductionOrderReadDto> data = productionOrderService.getOrders(keyword, status, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<ProductionOrderReadDto>> getOrderById(
            @PathVariable Long id,
            Principal principal) {
        ProductionOrderReadDto data = productionOrderService.getOrderById(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Order retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN')")
    public ResponseEntity<ResponseDto<ProductionOrderReadDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderUpdateDto dto,
            Principal principal) {
        ProductionOrderReadDto data = productionOrderService.updateProductionOrderAndProduct(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Order updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:MANUFACTURING:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        productionOrderService.deleteById(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Order deleted successfully"));
    }
}