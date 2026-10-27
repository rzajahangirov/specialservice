package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductCreateDto;
import az.techvibeds.specialservice.dtos.productionOrder.*;
import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.enums.ProductionOrderStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ManufacturedProductService;
import az.techvibeds.specialservice.services.NotificationService;
import az.techvibeds.specialservice.services.ProductionOrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderServiceImpl implements ProductionOrderService {
    private final ProductionOrderRepository orderRepository;
    private final ManufacturedProductService productService;
    private final CompanyService companyService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseActivityRepository warehouseActivityRepository;
    private final SemiProductRepository semiProductRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ProductionOrderReadDto createProductionServiceAndProduct(ProductionOrderCreateDto dto, String email) {
        log.info("Creating production order for user: {}", email);
        Company company = companyService.findByUserEmail(email);

        ProductionOrder order = new ProductionOrder();
        order.setCompany(company);
        order.setStartDate(dto.getStartDate());
        order.setFinishDate(dto.getFinishDate());
        order.setProgressPercentage(0);
        order.setStatus(dto.getOrderStatus());

        if (dto.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new BusinessException("Warehouse not found", HttpStatus.NOT_FOUND));
            if (!warehouse.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("Warehouse does not belong to your company");
            }
            order.setWarehouse(warehouse);
        }

        if (dto.getItems() != null) {
            for (ProductionOrderItemCreateDto itemDto : dto.getItems()) {
                ManufacturedProduct manufacturedProduct = productService.createProduct(itemDto.getManufacturedProduct(), email);
                // VAT sahələrini ManufacturedProduct-a yaz
                applyVatToManufacturedProduct(manufacturedProduct, itemDto.getManufacturedProduct());

                ProductionOrderItem item = new ProductionOrderItem();
                item.setManufacturedProduct(manufacturedProduct);
                item.setQuantity(itemDto.getQuantity());
                item.setMaterialQuantity(itemDto.getMaterialQuantity());
                item.setStockApplied(false);
                setRelations(item, itemDto.getProductId(), itemDto.getMaterialProductId(), company);

                if (itemDto.getIngredients() != null) {
                    for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto ingDto : itemDto.getIngredients()) {
                        ProductionOrderIngredient ingredient = new ProductionOrderIngredient();
                        ingredient.setQuantity(ingDto.getQuantity());
                        ingredient.setStockApplied(false);

                        Warehouse wh = warehouseRepository.findById(ingDto.getWarehouseId())
                                .orElseThrow(() -> new BusinessException("Ingredient warehouse not found: " + ingDto.getWarehouseId(), HttpStatus.NOT_FOUND));
                        if (!wh.getCompany().getId().equals(company.getId())) {
                            throw new AccessDeniedException("Ingredient warehouse does not belong to your company");
                        }
                        ingredient.setWarehouse(wh);

                        if (ingDto.getProductId() != null) {
                            Product p = productRepository.findById(ingDto.getProductId())
                                    .orElseThrow(() -> new BusinessException("Ingredient product not found: " + ingDto.getProductId(), HttpStatus.NOT_FOUND));
                            ingredient.setProduct(p);
                        }
                        item.addIngredient(ingredient);
                    }
                }

                if (itemDto.getExpenses() != null) {
                    for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderExpenseDto expDto : itemDto.getExpenses()) {
                        ProductionOrderExpense expense = new ProductionOrderExpense();
                        expense.setName(expDto.getName());
                        expense.setAmount(expDto.getAmount());
                        item.addExpense(expense);
                    }
                }

                order.addItem(item);
            }
        }

        applyStockIfCompleted(order, company);

        return mapToRead(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderReadDto> getOrders(String keyword, ProductionOrderStatus status, String email) {
        Company company = companyService.findByUserEmail(email);
        List<ProductionOrder> orders;
        
        if (company == null) {
            // Super Admin sees all orders
            if (keyword == null || keyword.isBlank()) {
                orders = orderRepository.findAll().stream()
                        .filter(o -> status == null || o.getStatus() == status)
                        .collect(Collectors.toList());
            } else {
                orders = orderRepository.findAll().stream()
                        .filter(o -> status == null || o.getStatus() == status)
                        .filter(o -> o.getItems().stream().anyMatch(item -> item.getManufacturedProduct().getName().contains(keyword)))
                        .collect(Collectors.toList());
            }
        } else {
            Long companyId = company.getId();
            if (keyword == null || keyword.isBlank()) {
                orders = orderRepository.findFilteredOrdersWithoutKeyword(companyId, status);
            } else {
                orders = orderRepository.findFilteredOrdersWithKeyword(companyId, status, keyword);
            }
        }

        return orders.stream()
                .map(this::mapToRead)
                .collect(Collectors.toList());
    }

    @Override
    public ProductionOrderReadDto getOrderById(Long id, String email) {
        ProductionOrder order = findAndValidateOwner(id, email);
        return mapToRead(order);
    }

    @Override
    @Transactional
    public ProductionOrderReadDto updateProductionOrderAndProduct(Long id, ProductionOrderUpdateDto dto, String email) {
        ProductionOrder order = findAndValidateOwner(id, email);
        Company company = order.getCompany();

        validateStockAppliedUpdate(order, dto);

        order.setStartDate(dto.getStartDate());
        order.setFinishDate(dto.getFinishDate());
        order.setProgressPercentage(dto.getProgressPercentage());
        order.setStatus(dto.getOrderStatus());

        if (dto.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new BusinessException("Warehouse not found", HttpStatus.NOT_FOUND));
            if (!warehouse.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("Warehouse does not belong to your company");
            }
            order.setWarehouse(warehouse);
        } else {
            order.setWarehouse(null);
        }

        if (dto.getItems() != null) {
            List<Long> incomingItemIds = dto.getItems().stream()
                    .map(ProductionOrderItemUpdateDto::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            order.getItems().removeIf(item -> !incomingItemIds.contains(item.getId()));

            for (ProductionOrderItemUpdateDto itemDto : dto.getItems()) {
                if (itemDto.getId() != null) {
                    ProductionOrderItem existingItem = order.getItems().stream()
                            .filter(item -> item.getId().equals(itemDto.getId()))
                            .findFirst()
                            .orElseThrow(() -> new EntityNotFoundException("Production Order Item not found: " + itemDto.getId()));

                    existingItem.setQuantity(itemDto.getQuantity());
                    existingItem.setMaterialQuantity(itemDto.getMaterialQuantity());
                    setRelations(existingItem, itemDto.getProductId(), itemDto.getMaterialProductId(), company);

                    if (itemDto.getProduct() != null) {
                        ManufacturedProduct product = existingItem.getManufacturedProduct();
                        product.setName(itemDto.getProduct().getName());
                        product.setDescription(itemDto.getProduct().getDescription());
                        // VAT sahələrini yenilə
                        applyVatToManufacturedProductUpdate(product, itemDto.getProduct());
                    }

                    existingItem.getIngredients().clear();
                    if (itemDto.getIngredients() != null) {
                        for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto ingDto : itemDto.getIngredients()) {
                            ProductionOrderIngredient ingredient = new ProductionOrderIngredient();
                            ingredient.setQuantity(ingDto.getQuantity());
                            ingredient.setStockApplied(false);

                            Warehouse wh = warehouseRepository.findById(ingDto.getWarehouseId())
                                    .orElseThrow(() -> new BusinessException("Ingredient warehouse not found: " + ingDto.getWarehouseId(), HttpStatus.NOT_FOUND));
                            ingredient.setWarehouse(wh);

                            if (ingDto.getProductId() != null) {
                                Product p = productRepository.findById(ingDto.getProductId())
                                        .orElseThrow(() -> new BusinessException("Ingredient product not found: " + ingDto.getProductId(), HttpStatus.NOT_FOUND));
                                ingredient.setProduct(p);
                            }
                            existingItem.addIngredient(ingredient);
                        }
                    }

                    existingItem.getExpenses().clear();
                    if (itemDto.getExpenses() != null) {
                        for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderExpenseDto expDto : itemDto.getExpenses()) {
                            ProductionOrderExpense expense = new ProductionOrderExpense();
                            expense.setName(expDto.getName());
                            expense.setAmount(expDto.getAmount());
                            existingItem.addExpense(expense);
                        }
                    }
                } else {
                    ManufacturedProduct manufacturedProduct = new ManufacturedProduct();
                    manufacturedProduct.setName(itemDto.getProduct().getName());
                    manufacturedProduct.setDescription(itemDto.getProduct().getDescription());
                    manufacturedProduct.setCompany(company);
                    // VAT sahələrini yeni ManufacturedProduct-a yaz
                    applyVatToManufacturedProductUpdate(manufacturedProduct, itemDto.getProduct());

                    ProductionOrderItem newItem = new ProductionOrderItem();
                    newItem.setManufacturedProduct(manufacturedProduct);
                    newItem.setQuantity(itemDto.getQuantity());
                    newItem.setMaterialQuantity(itemDto.getMaterialQuantity());
                    newItem.setStockApplied(false);
                    setRelations(newItem, itemDto.getProductId(), itemDto.getMaterialProductId(), company);

                    if (itemDto.getIngredients() != null) {
                        for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto ingDto : itemDto.getIngredients()) {
                            ProductionOrderIngredient ingredient = new ProductionOrderIngredient();
                            ingredient.setQuantity(ingDto.getQuantity());
                            ingredient.setStockApplied(false);

                            Warehouse wh = warehouseRepository.findById(ingDto.getWarehouseId())
                                    .orElseThrow(() -> new BusinessException("Ingredient warehouse not found: " + ingDto.getWarehouseId(), HttpStatus.NOT_FOUND));
                            ingredient.setWarehouse(wh);

                            if (ingDto.getProductId() != null) {
                                Product p = productRepository.findById(ingDto.getProductId())
                                        .orElseThrow(() -> new BusinessException("Ingredient product not found: " + ingDto.getProductId(), HttpStatus.NOT_FOUND));
                                ingredient.setProduct(p);
                            }
                            newItem.addIngredient(ingredient);
                        }
                    }

                    if (itemDto.getExpenses() != null) {
                        for (az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderExpenseDto expDto : itemDto.getExpenses()) {
                            ProductionOrderExpense expense = new ProductionOrderExpense();
                            expense.setName(expDto.getName());
                            expense.setAmount(expDto.getAmount());
                            newItem.addExpense(expense);
                        }
                    }

                    order.addItem(newItem);
                }
            }
        }

        applyStockIfCompleted(order, company);

        if (order.getStatus() == ProductionOrderStatus.COMPLETED) {
            notificationService.createEmailNotification(
                String.format("İstehsalat sifarişi tamamlandı. Sifariş ID: %d", order.getId()),
                company
            );
        }

        log.info("Production order ID: {} updated successfully", id);
        return mapToRead(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteById(Long id, String email) {
        ProductionOrder order = findAndValidateOwner(id, email);
        boolean anyStockApplied = order.getItems().stream().anyMatch(poi -> Boolean.TRUE.equals(poi.getStockApplied()));
        if (anyStockApplied) {
            throw new BusinessException("Completed production stock is already applied and cannot be deleted");
        }
        orderRepository.delete(order);
        log.info("Production order ID: {} deleted by {}", id, email);
    }

    private ProductionOrder findAndValidateOwner(Long id, String email) {
        ProductionOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Production Order not found"));

        Company userCompany = companyService.findByUserEmail(email);
        if (userCompany != null && !order.getCompany().getId().equals(userCompany.getId())) {
            throw new AccessDeniedException("Access denied to this order");
        } else if (userCompany == null && !userRepository.findByEmail(email).map(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))).orElse(false)) {
             throw new AccessDeniedException("Access denied to this order");
        }
        return order;
    }

    private ProductionOrderReadDto mapToRead(ProductionOrder order) {
        ProductionOrderReadDto readDto = new ProductionOrderReadDto();
        readDto.setId(order.getId());
        readDto.setOrderStatus(order.getStatus().name());
        readDto.setStartDate(order.getStartDate());
        readDto.setFinishDate(order.getFinishDate());
        readDto.setProgressPercentage(order.getProgressPercentage());

        if (order.getWarehouse() != null) {
            readDto.setWarehouseId(order.getWarehouse().getId());
            readDto.setWarehouseName(order.getWarehouse().getName());
        }

        if (order.getItems() != null) {
            List<ProductionOrderItemReadDto> itemDtos = order.getItems().stream()
                    .map(item -> {
                        ProductionOrderItemReadDto itemDto = new ProductionOrderItemReadDto();
                        itemDto.setId(item.getId());
                        itemDto.setProductName(item.getManufacturedProduct().getName());
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setMaterialQuantity(item.getMaterialQuantity());
                        itemDto.setStockApplied(item.getStockApplied());

                        if (item.getProduct() != null) {
                            itemDto.setInventoryProductId(item.getProduct().getId());
                            itemDto.setInventoryProductName(item.getProduct().getName());
                            itemDto.setInventoryProductCode(item.getProduct().getProductCode());
                        }
                        if (item.getMaterialProduct() != null) {
                            itemDto.setMaterialProductId(item.getMaterialProduct().getId());
                            itemDto.setMaterialProductName(item.getMaterialProduct().getName());
                            itemDto.setMaterialProductCode(item.getMaterialProduct().getProductCode());
                        }

                        if (item.getIngredients() != null) {
                            List<az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto> ingDtos = item.getIngredients().stream()
                                    .map(ing -> {
                                        az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto ingDto = new az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderIngredientDto();
                                        ingDto.setId(ing.getId());
                                        ingDto.setQuantity(ing.getQuantity());
                                        ingDto.setWarehouseId(ing.getWarehouse().getId());
                                        ingDto.setWarehouseName(ing.getWarehouse().getName());
                                        if (ing.getProduct() != null) {
                                            ingDto.setProductId(ing.getProduct().getId());
                                            ingDto.setProductName(ing.getProduct().getName());
                                        }
                                        return ingDto;
                                    })
                                    .collect(Collectors.toList());
                            itemDto.setIngredients(ingDtos);
                        }

                        if (item.getExpenses() != null) {
                            List<az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderExpenseDto> expDtos = item.getExpenses().stream()
                                    .map(exp -> new az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderExpenseDto(exp.getId(), exp.getName(), exp.getAmount()))
                                    .collect(Collectors.toList());
                            itemDto.setExpenses(expDtos);
                        }
                        itemDto.setPrimeCost(item.getPrimeCost());

                        // VAT fields
                        if (item.getManufacturedProduct() != null) {
                            itemDto.setPrice(item.getManufacturedProduct().getPrice());
                            itemDto.setVatStatus(item.getManufacturedProduct().getVatStatus() != null ? item.getManufacturedProduct().getVatStatus().name() : null);
                            itemDto.setVatRate(item.getManufacturedProduct().getVatRate());
                            itemDto.setPriceWithVat(item.getManufacturedProduct().getPriceWithVat());
                        }

                        return itemDto;
                    })
                    .collect(Collectors.toList());
            readDto.setItems(itemDtos);
        }

        return readDto;
    }

    private void setRelations(
            ProductionOrderItem item,
            Long productId,
            Long materialProductId,
            Company company) {
        if (productId != null) {
            item.setProduct(findProduct(productId, company, "Product"));
        }
        if (materialProductId != null) {
            item.setMaterialProduct(findProduct(materialProductId, company, "Material"));
        }
    }

    private Product findProduct(Long productId, Company company, String label) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(label + " not found", HttpStatus.NOT_FOUND));

        if (product.getCompany() == null || !product.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException(label + " does not belong to your company");
        }
        return product;
    }

    private SemiProduct findSemiProduct(Long productId, Company company, String label) {
        SemiProduct product = semiProductRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(label + " not found", HttpStatus.NOT_FOUND));

        if (product.getCompany() == null || !product.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException(label + " does not belong to your company");
        }
        return product;
    }

    private void applyStockIfCompleted(ProductionOrder order, Company company) {
        if (order.getStatus() != ProductionOrderStatus.COMPLETED) {
            return;
        }

        if (order.getWarehouse() == null) {
            throw new BusinessException("Warehouse is required to apply production stock");
        }

        for (ProductionOrderItem item : order.getItems()) {
            if (Boolean.TRUE.equals(item.getStockApplied())) {
                continue;
            }

            if (item.getIngredients() != null && !item.getIngredients().isEmpty()) {
                for (ProductionOrderIngredient ingredient : item.getIngredients()) {
                    if (Boolean.TRUE.equals(ingredient.getStockApplied())) {
                        continue;
                    }

                    if (ingredient.getProduct() != null) {
                        WarehouseProduct ingStock = warehouseProductRepository
                                .findByProductAndWarehouse(ingredient.getProduct(), ingredient.getWarehouse());
                        if (ingStock == null) {
                            throw new BusinessException("Ingredient (Product) " + ingredient.getProduct().getName() + " is not present in selected warehouse " + ingredient.getWarehouse().getName());
                        }

                        int reqQty = ingredient.getQuantity().intValue();
                        int available = ingStock.getQuantity() != null ? ingStock.getQuantity() : 0;
                        if (available < reqQty) {
                            throw new BusinessException("Not enough ingredient stock for " + ingredient.getProduct().getName() + " in " + ingredient.getWarehouse().getName() + ". Available: " + available + ", Required: " + reqQty);
                        }

                        ingStock.setQuantity(available - reqQty);
                        long currentStock = ingredient.getProduct().getStock() != null ? ingredient.getProduct().getStock() : 0L;
                        long newStock = Math.max(0, currentStock - reqQty);
                        ingredient.getProduct().setStock(newStock);
                        
                        Long limit = ingredient.getProduct().getMinStockLimit() != null ? ingredient.getProduct().getMinStockLimit() : 0L;
                        if (newStock <= 0) {
                            ingredient.getProduct().setProductStatus(ProductStatus.LOW_STOCK);
                        }
                        
                        if (newStock < limit && currentStock >= limit) {
                            notificationService.createEmailNotification(
                                String.format("Məhsul stoku limitdən aşağı düşdü: %s (Cari stok: %d, Limit: %d)", ingredient.getProduct().getName(), newStock, limit),
                                company
                            );
                        }

                        warehouseProductRepository.save(ingStock);
                        productRepository.save(ingredient.getProduct());
                        saveWarehouseActivity(ActivityTypeWarehouse.OUTGOING, ingStock, reqQty,
                                ingredient.getWarehouse(), null,
                                ingredient.getProduct().getPrice(), null,
                                "PRODUCTION-" + order.getId(), ingredient.getWarehouse().getName(),
                                "İstehsalat (Sifariş: " + order.getId() + ")", company);
                        ingredient.setStockApplied(true);
                    }
                }
            } else {
                if (item.getProduct() == null && item.getMaterialProduct() == null) {
                    continue;
                }

                if (item.getMaterialProduct() != null) {
                    int materialQuantity = item.getMaterialQuantity() != null && item.getMaterialQuantity() > 0
                            ? item.getMaterialQuantity()
                            : item.getQuantity();
                    WarehouseProduct materialStock = warehouseProductRepository
                            .findByProductAndWarehouse(item.getMaterialProduct(), order.getWarehouse());
                    if (materialStock == null) {
                        throw new BusinessException("Material product is not present in selected warehouse");
                    }

                    int available = materialStock.getQuantity() != null ? materialStock.getQuantity() : 0;
                    if (available < materialQuantity) {
                        throw new BusinessException("Not enough material stock. Available: " + available);
                    }

                    materialStock.setQuantity(available - materialQuantity);
                    // Product stock azalt
                    long currentStock = item.getMaterialProduct().getStock() != null ? item.getMaterialProduct().getStock() : 0L;
                    long newStock = Math.max(0, currentStock - materialQuantity);
                    item.getMaterialProduct().setStock(newStock);
                    
                    Long limit = item.getMaterialProduct().getMinStockLimit() != null ? item.getMaterialProduct().getMinStockLimit() : 0L;
                    if (newStock <= 0) {
                        item.getMaterialProduct().setProductStatus(ProductStatus.LOW_STOCK);
                    }
                    if (newStock < limit && currentStock >= limit) {
                        notificationService.createEmailNotification(
                            String.format("Məhsul stoku limitdən aşağı düşdü: %s (Cari stok: %d, Limit: %d)", item.getMaterialProduct().getName(), newStock, limit),
                            company
                        );
                    }
                    
                    warehouseProductRepository.save(materialStock);
                    productRepository.save(item.getMaterialProduct());
                    saveWarehouseActivity(ActivityTypeWarehouse.OUTGOING, materialStock, materialQuantity,
                            order.getWarehouse(), null,
                            item.getMaterialProduct().getPrice(), null,
                            "PRODUCTION-" + order.getId(), order.getWarehouse().getName(),
                            "İstehsalat (Sifariş: " + order.getId() + ")", company);
                    item.setMaterialQuantity(materialQuantity);
                }
            }

            if (item.getProduct() != null) {
                WarehouseProduct productStock = warehouseProductRepository
                        .findByProductAndWarehouse(item.getProduct(), order.getWarehouse());
                if (productStock == null) {
                    productStock = new WarehouseProduct();
                    productStock.setProduct(item.getProduct());
                    productStock.setWarehouse(order.getWarehouse());
                    productStock.setQuantity(0);
                }

                int currentQuantity = productStock.getQuantity() != null ? productStock.getQuantity() : 0;
                productStock.setQuantity(currentQuantity + item.getQuantity());
                increaseProductStock(item.getProduct(), item.getQuantity());
                warehouseProductRepository.save(productStock);
                productRepository.save(item.getProduct());
                saveWarehouseActivity(ActivityTypeWarehouse.INCOMING, productStock, item.getQuantity(),
                        null, order.getWarehouse(),
                        item.getPrimeCost(), item.getProduct().getPrice(),
                        "PRODUCTION-" + order.getId(), "İstehsalat",
                        order.getWarehouse().getName(), company);
            }

            item.setStockApplied(true);

            java.math.BigDecimal totalIngredientCost = java.math.BigDecimal.ZERO;
            if (item.getIngredients() != null) {
                for (ProductionOrderIngredient ingredient : item.getIngredients()) {
                    if (ingredient.getProduct() != null && ingredient.getProduct().getPrice() != null) {
                        totalIngredientCost = totalIngredientCost.add(ingredient.getProduct().getPrice().multiply(ingredient.getQuantity()));
                    }
                }
            }
            java.math.BigDecimal totalExpense = java.math.BigDecimal.ZERO;
            if (item.getExpenses() != null) {
                for (ProductionOrderExpense exp : item.getExpenses()) {
                    if (exp.getAmount() != null) {
                        totalExpense = totalExpense.add(exp.getAmount());
                    }
                }
            }
            java.math.BigDecimal totalCost = totalIngredientCost.add(totalExpense);
            item.setPrimeCost(totalCost);
            if (item.getManufacturedProduct() != null) {
                item.getManufacturedProduct().setPrimeCost(totalCost);
            }
        }
    }

    private void validateStockAppliedUpdate(ProductionOrder order, ProductionOrderUpdateDto dto) {
        boolean anyStockApplied = order.getItems().stream().anyMatch(poi -> Boolean.TRUE.equals(poi.getStockApplied()));
        if (!anyStockApplied) {
            return;
        }

        if (dto.getOrderStatus() != ProductionOrderStatus.COMPLETED) {
            throw new BusinessException("Completed production stock is already applied and cannot be changed");
        }
    }

    private void increaseProductStock(Product product, int quantity) {
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        product.setStock(currentStock + quantity);
        product.setProductStatus(ProductStatus.IN_STOCK);
    }

    private void decreaseSemiProductStock(SemiProduct semiProduct, int quantity) {
        long currentStock = semiProduct.getStock() != null ? semiProduct.getStock() : 0L;
        semiProduct.setStock(Math.max(0, currentStock - quantity));
    }

    private void saveWarehouseActivity(
            ActivityTypeWarehouse type,
            WarehouseProduct warehouseProduct,
            int quantity,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            java.math.BigDecimal purchasePrice,
            java.math.BigDecimal salePrice,
            String referenceNumber,
            String sourceName,
            String destinationName,
            Company company) {
        WarehouseActivity activity = new WarehouseActivity();
        activity.setType(type);
        activity.setProduct(warehouseProduct);
        activity.setQuantity(quantity);
        activity.setSourceWarehouse(sourceWarehouse);
        activity.setDestinationWarehouse(destinationWarehouse);
        activity.setPurchasePrice(purchasePrice);
        activity.setSalePrice(salePrice);
        activity.setReferenceNumber(referenceNumber);
        activity.setSourceName(sourceName);
        activity.setDestinationName(destinationName);
        activity.setDate(LocalDateTime.now());
        activity.setCompany(company);
        warehouseActivityRepository.save(activity);
    }

    /**
     * ManufacturedProductCreateDto-dan VAT sahələrini ManufacturedProduct-a yazır.
     */
    private void applyVatToManufacturedProduct(ManufacturedProduct product, ManufacturedProductCreateDto dto) {
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getVatStatus() != null) {
            try {
                product.setVatStatus(az.techvibeds.specialservice.enums.VatStatus.fromString(dto.getVatStatus()));
            } catch (Exception e) {
                product.setVatStatus(az.techvibeds.specialservice.enums.VatStatus.WITHOUT_VAT);
            }
        }
        if (dto.getVatRate() != null) {
            product.setVatRate(dto.getVatRate());
        }
    }

    /**
     * ManufacturedProductUpdateDto-dan VAT sahələrini ManufacturedProduct-a yazır.
     */
    private void applyVatToManufacturedProductUpdate(ManufacturedProduct product, az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductUpdateDto dto) {
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getVatStatus() != null) {
            try {
                product.setVatStatus(az.techvibeds.specialservice.enums.VatStatus.fromString(dto.getVatStatus()));
            } catch (Exception e) {
                product.setVatStatus(az.techvibeds.specialservice.enums.VatStatus.WITHOUT_VAT);
            }
        }
        if (dto.getVatRate() != null) {
            product.setVatRate(dto.getVatRate());
        }
    }
}
