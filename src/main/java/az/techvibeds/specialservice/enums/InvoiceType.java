package az.techvibeds.specialservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Faktura növləri.
 * SALES    – Satış fakturası (müştəriyə kəsilir)
 * PURCHASE – Alış fakturası (təchizatçıdan alınır)
 */
public enum InvoiceType {
    SALES,
    PURCHASE;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvoiceType fromValue(String value) {
        return InvoiceType.valueOf(value.toUpperCase());
    }
}
