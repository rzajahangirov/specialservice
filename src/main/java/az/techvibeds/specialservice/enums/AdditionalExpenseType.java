package az.techvibeds.specialservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Əlavə xərcin növü.
 * AMOUNT     – Sabit məbləğ kimi əlavə xərc
 * PERCENTAGE – Ümumi məbləğə nisbətdə faiz kimi əlavə xərc
 */
public enum AdditionalExpenseType {
    AMOUNT,
    PERCENTAGE;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static AdditionalExpenseType fromValue(String value) {
        if (value == null || value.isBlank()) return AMOUNT;
        return AdditionalExpenseType.valueOf(value.toUpperCase());
    }
}
