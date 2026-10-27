package az.techvibeds.specialservice.enums;

import lombok.Getter;

@Getter
public enum VatStatus {
    WITH_VAT("ƏDV-li"),
    WITHOUT_VAT("ƏDV-siz");

    private final String description;

    VatStatus(String description) {
        this.description = description;
    }

    public static VatStatus fromString(String value) {
        if (value == null) return WITHOUT_VAT;
        for (VatStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return WITHOUT_VAT;
    }
}
