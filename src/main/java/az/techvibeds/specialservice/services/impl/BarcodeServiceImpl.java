package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.barcode.BarcodeSearchResultDto;
import az.techvibeds.specialservice.dtos.barcode.WarehouseStockDto;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Product;
import az.techvibeds.specialservice.models.SemiProduct;
import az.techvibeds.specialservice.repositories.ProductRepository;
import az.techvibeds.specialservice.repositories.SemiProductRepository;
import az.techvibeds.specialservice.services.BarcodeService;
import az.techvibeds.specialservice.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarcodeServiceImpl implements BarcodeService {

    private final ProductRepository productRepository;
    private final SemiProductRepository semiProductRepository;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public BarcodeSearchResultDto searchByBarcode(String barcode, String userEmail) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new BusinessException("Barkod boş ola bilməz!", HttpStatus.BAD_REQUEST);
        }

        Company company = companyService.findByUserEmail(userEmail);
        if (company == null) {
            throw new BusinessException("Şirkət tapılmadı!", HttpStatus.NOT_FOUND);
        }

        Long companyId = company.getId();

        // 1. Məhsullarda axtar
        Optional<Product> productOpt = productRepository.findFirstByProductCodeAndCompanyIdOrderByIdAsc(barcode.trim(), companyId);
        if (productOpt.isPresent()) {
            return mapProductToDto(productOpt.get());
        }

        // 2. Yarım-məhsullarda (SemiProduct) axtar
        Optional<SemiProduct> semiProductOpt = semiProductRepository.findByProductCodeAndCompany_Id(barcode.trim(), companyId);
        if (semiProductOpt.isPresent()) {
            return mapSemiProductToDto(semiProductOpt.get());
        }

        throw new BusinessException("Bu barkoda uyğun məhsul və ya mal tapılmadı: " + barcode, HttpStatus.NOT_FOUND);
    }

    private BarcodeSearchResultDto mapProductToDto(Product product) {
        List<WarehouseStockDto> warehouseStocks = product.getWarehouseProducts().stream()
                .filter(wp -> wp.getWarehouse() != null)
                .map(wp -> WarehouseStockDto.builder()
                        .warehouseId(wp.getWarehouse().getId())
                        .warehouseName(wp.getWarehouse().getName())
                        .quantity(wp.getQuantity())
                        .build())
                .toList();

        return BarcodeSearchResultDto.builder()
                .id(product.getId())
                .name(product.getName())
                .barcode(product.getProductCode())
                .type(product.getProductType() != null ? product.getProductType().name() : "GOODS")
                .price(product.getPrice())
                .priceWithVat(product.getPriceWithVat())
                .vatRate(product.getVatRate())
                .vatStatus(product.getVatStatus() != null ? product.getVatStatus().name() : "WITHOUT_VAT")
                .unit(product.getUnit() != null ? product.getUnit().getName() : null)
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .status(product.getProductStatus() != null ? product.getProductStatus().name() : null)
                .minStockLimit(product.getMinStockLimit())
                .totalStock(product.getStock())
                .warehouseStocks(warehouseStocks)
                .build();
    }

    private BarcodeSearchResultDto mapSemiProductToDto(SemiProduct semiProduct) {
        List<WarehouseStockDto> warehouseStocks = semiProduct.getWarehouseProducts().stream()
                .filter(wp -> wp.getWarehouse() != null)
                .map(wp -> WarehouseStockDto.builder()
                        .warehouseId(wp.getWarehouse().getId())
                        .warehouseName(wp.getWarehouse().getName())
                        .quantity(wp.getQuantity())
                        .build())
                .toList();

        return BarcodeSearchResultDto.builder()
                .id(semiProduct.getId())
                .name(semiProduct.getName())
                .barcode(semiProduct.getProductCode())
                .type("SEMI_PRODUCT")
                .price(semiProduct.getPrice())
                .priceWithVat(semiProduct.getPriceWithVat())
                .vatRate(semiProduct.getVatRate())
                .vatStatus(semiProduct.getVatStatus() != null ? semiProduct.getVatStatus().name() : "WITHOUT_VAT")
                .unit(semiProduct.getUnit() != null ? semiProduct.getUnit().name() : null)
                .category(semiProduct.getCategory() != null ? semiProduct.getCategory().getName() : null)
                .status(semiProduct.getStatus() != null ? semiProduct.getStatus().name() : null)
                .minStockLimit(null)
                .totalStock(semiProduct.getStock())
                .warehouseStocks(warehouseStocks)
                .build();
    }
}
