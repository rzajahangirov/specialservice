package az.techvibeds.specialservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Ödəniş metodları.
 * CASH           – Nağd ödəniş
 * BANK_TRANSFER  – Bank köçürməsi
 * CARD           – Kart ilə ödəniş
 * OTHER          – Digər
 */
public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD,
    OTHER;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        return PaymentMethod.valueOf(value.toUpperCase());
    }
}
