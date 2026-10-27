package az.techvibeds.specialservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Xarici barkod bazalarÄ±ndan mÉ™hsul mÉ™lumatlarÄ±nÄ± axtarÄ±r.
 * Open Food Facts (pulsuz, aÃ§Ä±q API) istifadÉ™ edir.
 * 
 * Barkod oxuduqda mÉ™hsul sistemdÉ™ yoxdursa, bu servis vasitÉ™silÉ™
 * mÉ™hsulun adÄ±nÄ± avtomatik tapÄ±rÄ±q.
 */
@Slf4j
@Service
public class ExternalBarcodeService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Xarici bazadan barkod Ã¼zrÉ™ mÉ™hsul adÄ±nÄ± tapÄ±r.
     * TapÄ±lmazsa null qaytarÄ±r.
     */
    @SuppressWarnings("rawtypes")
    public String lookupProductName(String barcode) {
        try {
            String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";
            
            HttpHeaders headers = new HttpHeaders();
            // Open Food Facts Ã¼Ã§Ã¼n mÉ™cburi User-Agent tÉ™yin edirik ki, Cloudflare/API sorÄŸunu bloklamasÄ±n
            headers.set("User-Agent", "SpecialServicePOS/1.0 (jahangirovmahammadrza@gmail.com) Java/17");
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map response = responseEntity.getBody();
            
            if (response != null && ("product_found".equals(response.get("status_verbose")) || Integer.valueOf(1).equals(response.get("status")))) {
                Map product = (Map) response.get("product");
                if (product != null) {
                    // AzÉ™rbaycan, TÃ¼rk, Rus dillÉ™ri vÉ™ ya É™sas ad Ã¼zrÉ™ axtarÄ±ÅŸ edirik
                    String name = (String) product.get("product_name_az");
                    if (name == null || name.isBlank()) {
                        name = (String) product.get("product_name_tr");
                    }
                    if (name == null || name.isBlank()) {
                        name = (String) product.get("product_name");
                    }
                    if (name == null || name.isBlank()) {
                        name = (String) product.get("product_name_en");
                    }
                    if (name == null || name.isBlank()) {
                        name = (String) product.get("generic_name");
                    }
                    
                    if (name != null && !name.isBlank()) {
                        log.info("Xarici bazadan mÉ™hsul tapÄ±ldÄ±: {} -> {}", barcode, name);
                        return name.trim();
                    }
                }
            }
            
            log.info("Xarici bazadan mÉ™hsul tapÄ±lmadÄ±: {}", barcode);
            return null;
            
        } catch (Exception e) {
            log.warn("Xarici barkod axtarÄ±ÅŸÄ± uÄŸursuz oldu: {} - {}", barcode, e.getMessage());
            return null;
        }
    }
}

