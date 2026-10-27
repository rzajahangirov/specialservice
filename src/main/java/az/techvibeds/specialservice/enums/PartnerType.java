package az.techvibeds.specialservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PartnerType {
    CUSTOMER,
    SUPPLIER;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static PartnerType fromValue(String value) {
        return PartnerType.valueOf(value.toUpperCase());
    }
}
