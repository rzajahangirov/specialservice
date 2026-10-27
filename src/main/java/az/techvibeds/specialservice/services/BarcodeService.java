package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.barcode.BarcodeSearchResultDto;

public interface BarcodeService {
    BarcodeSearchResultDto searchByBarcode(String barcode, String userEmail);
}
