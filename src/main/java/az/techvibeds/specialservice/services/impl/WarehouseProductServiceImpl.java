package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.enums.ProductType;
import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.ProductRepository;
import az.techvibeds.specialservice.repositories.WarehouseProductRepository;
import az.techvibeds.specialservice.repositories.WarehouseRepository;
import az.techvibeds.specialservice.repositories.WarehouseActivityRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.WarehouseProductService;
import az.techvibeds.specialservice.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseProductServiceImpl implements WarehouseProductService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;
    private final WarehouseActivityRepository warehouseActivityRepository;

    @Override
    @Transactional
    public WarehouseProduct createWarehouseProduct(Product product, Long warehouseId, double quantity) {
        Warehouse warehouse = warehouseService.getWarehouseById(warehouseId);

        Optional<WarehouseProduct> existingWp = warehouseProductRepository.findByProduct_ProductCodeAndWarehouse_IdAndProduct_Company_Id(
                product.getProductCode(), warehouseId, product.getCompany().getId()
        );

        if (existingWp.isPresent()) {
            WarehouseProduct wp = existingWp.get();
            if (product.getProductType() == ProductType.GOODS) {
                wp.setQuantity(null);
            } else {
                int currentQty = wp.getQuantity() != null ? wp.getQuantity() : 0;
                wp.setQuantity(currentQty + (int) quantity);
            }
            return warehouseProductRepository.save(wp);
        }

        WarehouseProduct warehouseProduct = new WarehouseProduct();
        warehouseProduct.setProduct(product);
        warehouseProduct.setWarehouse(warehouse);
        warehouseProduct.setCompany(product.getCompany());
        if (product.getProductType() == ProductType.GOODS) {
            warehouseProduct.setQuantity(null);
        } else {
            warehouseProduct.setQuantity((int) Math.max(0, quantity));
        }

        return warehouseProductRepository.save(warehouseProduct);
    }

    @Override
    @Transactional
    public WarehouseProduct createWarehouseSemiProduct(SemiProduct semiProduct, Long warehouseId, double quantity, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);

        if (!semiProduct.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Semi-product does not belong to your company", HttpStatus.FORBIDDEN);
        }

        Warehouse warehouse = warehouseService.getWarehouseById(warehouseId);
        if (!warehouse.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Warehouse does not belong to your company", HttpStatus.FORBIDDEN);
        }

        // Əgər bu mal artıq bu anbarda varsa, miqdarı artır
        WarehouseProduct existing = warehouseProductRepository.findBySemiProductAndWarehouse(semiProduct, warehouse);
        if (existing != null) {
            int currentQty = existing.getQuantity() != null ? existing.getQuantity() : 0;
            existing.setQuantity(currentQty + (int) quantity);
            log.info("Updated semi-product {} stock in warehouse {} to {}", semiProduct.getName(), warehouse.getName(), existing.getQuantity());
            return warehouseProductRepository.save(existing);
        }

        // Yeni anbar qeydi yarat
        WarehouseProduct warehouseProduct = new WarehouseProduct();
        warehouseProduct.setSemiProduct(semiProduct);
        warehouseProduct.setWarehouse(warehouse);
        warehouseProduct.setCompany(company);
        warehouseProduct.setQuantity((int) Math.max(0, quantity));

        log.info("Created new semi-product {} record in warehouse {} with qty {}", semiProduct.getName(), warehouse.getName(), (int) quantity);
        return warehouseProductRepository.save(warehouseProduct);
    }

    @Override
    @Transactional
    public WarehouseProduct createWarehouseProductFromExcel(Product product, String warehouseName, double quantity) {
        Warehouse warehouse = warehouseService.getWarehouseByName(warehouseName);

        // Əgər bu məhsul bu anbarda artıq varsa, dublikat yaratmaq əvəzinə miqdarı yenilə
        Optional<WarehouseProduct> existing = warehouseProductRepository
                .findByProduct_ProductCodeAndWarehouse_IdAndProduct_Company_Id(
                        product.getProductCode(), warehouse.getId(), product.getCompany().getId());

        if (existing.isPresent()) {
            WarehouseProduct wp = existing.get();
            if (product.getProductType() == ProductType.GOODS) {
                wp.setQuantity(null);
            } else {
                int currentQty = wp.getQuantity() != null ? wp.getQuantity() : 0;
                wp.setQuantity(currentQty + (int) Math.max(0, quantity));
            }
            log.info("Updated existing warehouse product {} in warehouse {} to qty {}",
                    product.getProductCode(), warehouseName, wp.getQuantity());
            return warehouseProductRepository.save(wp);
        }

        WarehouseProduct warehouseProduct = new WarehouseProduct();
        warehouseProduct.setProduct(product);
        warehouseProduct.setWarehouse(warehouse);
        warehouseProduct.setCompany(product.getCompany());
        if (product.getProductType() == ProductType.GOODS) {
            warehouseProduct.setQuantity(null);
        } else {
            warehouseProduct.setQuantity((int) Math.max(0, quantity));
        }

        log.info("Created new warehouse product {} in warehouse {} with qty {}",
                product.getProductCode(), warehouseName, product.getProductType() == ProductType.GOODS ? null : (int) quantity);
        return warehouseProductRepository.save(warehouseProduct);
    }

    @Override
    @Transactional
    public void removeFromWarehouse(String productCode, Long warehouseId, Integer quantity, String userEmail) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        Company company = companyService.findByUserEmail(userEmail);
        WarehouseProduct wp = findAndValidateOwnership(productCode, warehouseId, company.getId());

        int currentQty = wp.getQuantity() != null ? wp.getQuantity() : 0;
        if (currentQty < quantity) {
            throw new BusinessException("Not enough stock. Available: " + currentQty);
        }

        wp.setQuantity(currentQty - quantity);
        Product product = wp.getProduct();
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        product.setStock(currentStock - quantity);

        updateProductStatusIfLow(product);

        warehouseProductRepository.save(wp);
        productRepository.save(product);

        // Anbar hərəkəti loqu
        Warehouse wh = wp.getWarehouse();
        WarehouseActivity activity = new WarehouseActivity();
        activity.setType(ActivityTypeWarehouse.OUTGOING);
        activity.setProduct(wp);
        activity.setQuantity(quantity);
        activity.setSourceWarehouse(wh);
        activity.setSourceName(wh != null ? wh.getName() : null);
        activity.setDestinationName("Manual Silinmə");
        activity.setReferenceNumber("MANUAL");
        activity.setPurchasePrice(product.getPrice());
        activity.setSalePrice(product.getPrice());
        activity.setDate(java.time.LocalDateTime.now());
        activity.setCompany(company);
        warehouseActivityRepository.save(activity);

        log.info("Removed {} units of {} from warehouse {}", quantity, productCode, warehouseId);
    }

    @Override
    @Transactional
    public void transferProductBetweenWarehouses(String productCode, Long fromId, Long toId, int quantity, String userEmail) {
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        Company company = companyService.findByUserEmail(userEmail);

        WarehouseProduct fromWp = findAndValidateOwnership(productCode, fromId, company.getId());
        int fromQty = fromWp.getQuantity() != null ? fromWp.getQuantity() : 0;
        if (fromQty < quantity) {
            throw new BusinessException("Insufficient stock in source warehouse");
        }

        Warehouse toWarehouse = warehouseService.getWarehouseById(toId);
        if (!toWarehouse.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Target warehouse does not belong to your company", HttpStatus.FORBIDDEN);
        }

        WarehouseProduct toWp = warehouseProductRepository.findByProductAndWarehouse(fromWp.getProduct(), toWarehouse);
        if (toWp == null) {
            toWp = createWarehouseProduct(fromWp.getProduct(), toId, 0);
        }

        fromWp.setQuantity((fromWp.getQuantity() != null ? fromWp.getQuantity() : 0) - quantity);
        toWp.setQuantity((toWp.getQuantity() != null ? toWp.getQuantity() : 0) + quantity);

        warehouseProductRepository.save(fromWp);
        warehouseProductRepository.save(toWp);

        // Anbar hərəkəti loqu — Transfer
        Warehouse fromWarehouse = fromWp.getWarehouse();
        WarehouseActivity transferActivity = new WarehouseActivity();
        transferActivity.setType(ActivityTypeWarehouse.TRANSFER);
        transferActivity.setProduct(fromWp);
        transferActivity.setQuantity(quantity);
        transferActivity.setSourceWarehouse(fromWarehouse);
        transferActivity.setDestinationWarehouse(toWarehouse);
        transferActivity.setSourceName(fromWarehouse != null ? fromWarehouse.getName() : null);
        transferActivity.setDestinationName(toWarehouse.getName());
        transferActivity.setReferenceNumber("TRANSFER");
        transferActivity.setPurchasePrice(fromWp.getProduct().getPrice());
        transferActivity.setSalePrice(fromWp.getProduct().getPrice());
        transferActivity.setDate(java.time.LocalDateTime.now());
        transferActivity.setCompany(company);
        warehouseActivityRepository.save(transferActivity);

        log.info("Transferred {} units of {} from {} to {}", quantity, productCode, fromId, toId);
    }

    @Override
    @Transactional
    public Integer updateInventoryWarehouseProduct(WarehouseProduct wp, Long warehouseId, Integer quantity) {
        Integer previousQuantity = wp.getQuantity();
        Warehouse newWarehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException("Warehouse not found"));

        if (wp.getProduct() != null && wp.getProduct().getProductType() == ProductType.GOODS) {
            wp.setQuantity(null);
        } else {
            wp.setQuantity(quantity != null ? Math.max(0, quantity) : 0);
        }
        wp.setWarehouse(newWarehouse);

        warehouseProductRepository.save(wp);
        return previousQuantity;
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseProduct findWarehouseProductById(Long id) {
        return warehouseProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Warehouse-Product record not found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public WarehouseProduct saveWarehouseProduct(WarehouseProduct warehouseProduct) {
        return warehouseProductRepository.save(warehouseProduct);
    }

    @Override
    @Transactional
    public void deleteWarehouseProduct(Long id, String userEmail) {
        WarehouseProduct wp = findWarehouseProductById(id);
        Company company = companyService.findByUserEmail(userEmail);

        if (!wp.getProduct().getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        Product product = wp.getProduct();
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        int wpQty = wp.getQuantity() != null ? wp.getQuantity() : 0;
        product.setStock(currentStock - wpQty);

        productRepository.save(product);
        warehouseProductRepository.delete(wp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProduct> findWarehouseProductByCompany_Id(List<Warehouse> warehouseList) {
        List<WarehouseProduct> result = new ArrayList<>();
        for (Warehouse w : warehouseList) {
            result.addAll(warehouseProductRepository.findAllByWarehouse_Id(w.getId()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProduct> findWarehouseProductByWarehouseId(Long warehouseId) {
        return warehouseProductRepository.findAllByWarehouse_Id(warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProduct> findFiltered(Long companyId, Long warehouseId) {
        if (warehouseId == null) {
            return warehouseProductRepository.findAllByWarehouse_Company_Id(companyId);
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException("Warehouse not found"));

        if (!warehouse.getCompany().getId().equals(companyId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        return warehouseProductRepository.findAllByWarehouse_Id(warehouseId);
    }

    private WarehouseProduct findAndValidateOwnership(String productCode, Long warehouseId, Long companyId) {
        Product product = productRepository.findByProductCode(productCode);
        if (product == null || !product.getCompany().getId().equals(companyId)) {
            throw new BusinessException("Product not found or access denied");
        }

        Warehouse warehouse = warehouseService.getWarehouseById(warehouseId);
        if (!warehouse.getCompany().getId().equals(companyId)) {
            throw new BusinessException("Warehouse access denied", HttpStatus.FORBIDDEN);
        }

        WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(product, warehouse);
        if (wp == null) {
            throw new BusinessException("This product is not present in the specified warehouse");
        }
        return wp;
    }

    private void updateProductStatusIfLow(Product product) {
        long stock = product.getStock() != null ? product.getStock() : 0L;
        if (stock <= 0) {
            product.setProductStatus(ProductStatus.LOW_STOCK);
        }
    }

    @Override
    public WarehouseProduct findByProductAndWarehouse(Long productId, Long warehouseId) {
        return warehouseProductRepository.findByProduct_IdAndWarehouse_Id(productId, warehouseId)
                .orElse(null);
    }
}