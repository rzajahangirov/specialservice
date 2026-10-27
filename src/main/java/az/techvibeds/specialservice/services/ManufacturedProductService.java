package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductCreateDto;
import az.techvibeds.specialservice.models.ManufacturedProduct;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

public interface ManufacturedProductService {
    ManufacturedProduct createProduct(ManufacturedProductCreateDto manufacturedProduct, String userEmail);

    ByteArrayInputStream exportToExcel(String userEmail) throws Exception;

    void importFromExcel(MultipartFile file, String userEmail) throws Exception;
}
