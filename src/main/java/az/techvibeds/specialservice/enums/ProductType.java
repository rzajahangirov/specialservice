package az.techvibeds.specialservice.enums;

import lombok.Getter;

@Getter
public enum ProductType {
    PRODUCT("Məhsul"),
    GOODS("Mal");

    private final String description;

    ProductType(String description) {
        this.description = description;
    }
}
