package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.inventory.StockCountCreateDto;
import az.techvibeds.specialservice.dtos.inventory.StockCountItemDto;
import az.techvibeds.specialservice.dtos.inventory.StockCountItemReadDto;
import az.techvibeds.specialservice.dtos.inventory.StockCountReadDto;
import az.techvibeds.specialservice.enums.StockCountStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.StockCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCountServiceImpl implements StockCountService {

    private final StockCountRepository stockCountRepository;
    private final StockCountItemRepository stockCountItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseProductRepository warehouseProductRepository;
    private final CompanyService companyService;

    @Override
    @Transactional
    public StockCountReadDto createStockCount(StockCountCreateDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("User not found"));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found"));

        if (company != null && !warehouse.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Access denied to this warehouse", HttpStatus.FORBIDDEN);
        } else if (company == null) {
            // If super admin, we need to ensure they can create SC if needed, 
            // but normally SC belongs to a company. For now, let's just use warehouse's company.
             company = warehouse.getCompany();
        }

        StockCount stockCount = StockCount.builder()
                .countNumber("SC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .warehouse(warehouse)
                .countedBy(user)
                .countDate(LocalDateTime.now())
                .status(StockCountStatus.DRAFT)
                .note(dto.getNote())
                .company(company)
                .build();

        StockCount savedCount = stockCountRepository.save(stockCount);

        for (StockCountItemDto itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found: " + itemDto.getProductId()));

            WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(product, warehouse);
            int expectedQty = (wp != null) ? (wp.getQuantity() != null ? wp.getQuantity() : 0) : 0;

            StockCountItem item = StockCountItem.builder()
                    .product(product)
                    .expectedQuantity(expectedQty)
                    .countedQuantity(itemDto.getCountedQuantity())
                    .difference(itemDto.getCountedQuantity() - expectedQty)
                    .stockCount(savedCount)
                    .build();

            stockCountItemRepository.save(item);
            savedCount.getItems().add(item);
        }

        return mapToReadDto(savedCount);
    }

    @Override
    @Transactional
    public StockCountReadDto updateStockCount(Long id, StockCountCreateDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        StockCount stockCount;
        if (company != null) {
            stockCount = stockCountRepository.findByIdAndCompanyId(id, company.getId())
                    .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        } else {
            stockCount = stockCountRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        }

        if (stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new BusinessException("Only DRAFT stock counts can be updated");
        }

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found"));

        if (company != null && !warehouse.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Access denied to this warehouse", HttpStatus.FORBIDDEN);
        }

        stockCount.setWarehouse(warehouse);
        stockCount.setNote(dto.getNote());

        // Clear existing items
        stockCount.getItems().clear();
        stockCountRepository.save(stockCount);

        for (StockCountItemDto itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found: " + itemDto.getProductId()));

            WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(product, warehouse);
            int expectedQty = (wp != null) ? (wp.getQuantity() != null ? wp.getQuantity() : 0) : 0;

            StockCountItem item = StockCountItem.builder()
                    .product(product)
                    .expectedQuantity(expectedQty)
                    .countedQuantity(itemDto.getCountedQuantity())
                    .difference(itemDto.getCountedQuantity() - expectedQty)
                    .stockCount(stockCount)
                    .build();

            stockCount.addItem(item);
        }

        StockCount updatedCount = stockCountRepository.save(stockCount);
        return mapToReadDto(updatedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public StockCountReadDto getStockCountById(Long id, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        StockCount stockCount;
        if (company != null) {
            stockCount = stockCountRepository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        } else {
             stockCount = stockCountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        }
        return mapToReadDto(stockCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockCountReadDto> getAllStockCounts(String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        if (company == null) {
            // Super admin sees all stock counts
            return stockCountRepository.findAll().stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toList());
        }
        return stockCountRepository.findAllByCompanyId(company.getId()).stream()
                .map(this::mapToReadDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockCountReadDto completeStockCount(Long id, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        StockCount stockCount;
        if (company != null) {
            stockCount = stockCountRepository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        } else {
            stockCount = stockCountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        }

        if (stockCount.getStatus() != StockCountStatus.DRAFT) {
            throw new BusinessException("Only DRAFT stock counts can be completed");
        }

        // Update warehouse product quantities based on count
        for (StockCountItem item : stockCount.getItems()) {
            WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(item.getProduct(), stockCount.getWarehouse());
            
            if (wp == null) {
                wp = new WarehouseProduct();
                wp.setProduct(item.getProduct());
                wp.setWarehouse(stockCount.getWarehouse());
            }
            
            wp.setQuantity(item.getCountedQuantity());
            warehouseProductRepository.save(wp);

            // Update total product stock
            Product product = item.getProduct();
            Long totalStock = warehouseProductRepository.findAllByProduct(product).stream()
                    .mapToLong(w -> w.getQuantity() != null ? w.getQuantity() : 0L)
                    .sum();
            product.setStock(totalStock);
            productRepository.save(product);
        }

        stockCount.setStatus(StockCountStatus.COMPLETED);
        StockCount updated = stockCountRepository.save(stockCount);
        
        log.info("Stock count {} completed by {}", updated.getCountNumber(), userEmail);
        return mapToReadDto(updated);
    }

    @Override
    @Transactional
    public void deleteStockCount(Long id, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        StockCount stockCount;
        if (company != null) {
            stockCount = stockCountRepository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        } else {
            stockCount = stockCountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock count not found", HttpStatus.NOT_FOUND));
        }

        if (stockCount.getStatus() == StockCountStatus.COMPLETED) {
            throw new BusinessException("Completed stock counts cannot be deleted");
        }

        stockCountRepository.delete(stockCount);
    }

    private StockCountReadDto mapToReadDto(StockCount stockCount) {
        return StockCountReadDto.builder()
                .id(stockCount.getId())
                .countNumber(stockCount.getCountNumber())
                .warehouseId(stockCount.getWarehouse().getId())
                .warehouseName(stockCount.getWarehouse().getName())
                .countedByEmail(stockCount.getCountedBy().getEmail())
                .countDate(stockCount.getCountDate())
                .status(stockCount.getStatus())
                .note(stockCount.getNote())
                .items(stockCount.getItems().stream()
                        .map(item -> StockCountItemReadDto.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .productCode(item.getProduct().getProductCode())
                                .expectedQuantity(item.getExpectedQuantity())
                                .countedQuantity(item.getCountedQuantity())
                                .difference(item.getDifference())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
