package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.product.ProductCreateDto;
import az.techvibeds.specialservice.dtos.product.ProductInventoryDto;
import az.techvibeds.specialservice.dtos.product.ProductInventoryUpdateDto;
import az.techvibeds.specialservice.dtos.product.ProductReadDto;
import az.techvibeds.specialservice.models.Company;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.List;

public interface ProductService {
    Long calculateCompanyStock(Long companyId);

    Integer getProductCountByCompany(Long companyId);

    List<ProductInventoryDto> getProductsByCompany(String email, az.techvibeds.specialservice.enums.ProductType productType);

    void uploadProductsFromExcel(MultipartFile file, Company company) throws Exception;

    ProductReadDto createProductAndInventoryRecord(ProductCreateDto productCreateDto, Company byUserEmail) throws Exception;

    ProductReadDto updateInventorProduct(ProductInventoryUpdateDto productInventoryUpdateDto, String userEmail) throws Exception;

    List<ProductInventoryDto> getFilteredProducts(Long warehouseId, Long categoryId, String productStatus, String userEmail);

    List<ProductInventoryDto> getSearchedProducts(String productName, Principal principal);

    void addQuantityToProduct(String productCode, Long warehouseId, Integer quantity, String userEmail);

    ByteArrayInputStream exportProductsToExcel(String userEmail) throws Exception;

    /** Barkod oxutmaqla məhsulu sürətlə anbara əlavə edir (yoxdursa xarici bazadan tapıb yaradır) */
    ProductReadDto quickScanToInventory(String barcode, Long warehouseId, Long categoryId, String userEmail);
}
