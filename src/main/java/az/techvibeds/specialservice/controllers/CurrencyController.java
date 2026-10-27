package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.dtos.currency.CurrencyRateHistoryDto;
import az.techvibeds.specialservice.services.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @GetMapping("/{code}")
    public ResponseEntity<CurrencyDto> getCurrencyByCode(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.getCurrencyByCode(code));
    }

    @PostMapping
    public ResponseEntity<CurrencyDto> createOrUpdateCurrency(@RequestBody CurrencyDto dto) {
        return ResponseEntity.ok(currencyService.createOrUpdateCurrency(dto));
    }

    @GetMapping("/{code}/history")
    public ResponseEntity<List<CurrencyRateHistoryDto>> getHistory(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.getRateHistory(code));
    }
}
