package az.techvibeds.specialservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Faktura statusları.
 * DRAFT    – Qaralama (hələ göndərilməyib)
 * SENT     – Göndərilib (müştəriyə/təchizatçıya çatdırılıb)
 * PAID     – Ödənilib
 * OVERDUE  – Vaxtı keçib (ödəniş gecikir)
 * CANCELLED – Ləğv edilib
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    PARTIALLY_PAID,
    OVERDUE,
    CANCELLED;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvoiceStatus fromValue(String value) {
        return InvoiceStatus.valueOf(value.toUpperCase());
    }
}
