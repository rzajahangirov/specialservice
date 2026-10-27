package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.dtos.currency.CurrencyRateHistoryDto;

import java.util.List;

public interface CurrencyService {
    List<CurrencyDto> getAllCurrencies();
    CurrencyDto getCurrencyByCode(String code);
    CurrencyDto createOrUpdateCurrency(CurrencyDto dto);
    List<CurrencyRateHistoryDto> getRateHistory(String currencyCode);
}
