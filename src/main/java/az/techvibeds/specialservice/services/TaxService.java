package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.finance.TaxCalculateRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxCalculateResponseDto;
import az.techvibeds.specialservice.dtos.finance.TaxPaymentRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxXmlExportRequestDto;

public interface TaxService {
    TaxCalculateResponseDto calculateTax(TaxCalculateRequestDto request, String userEmail);

    TaxCalculateResponseDto payTax(TaxPaymentRequestDto request, String userEmail);

    byte[] exportXml(TaxXmlExportRequestDto request, String userEmail);
}
