package az.techvibeds.specialservice.enums;

import az.techvibeds.specialservice.exceptions.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ProductStatus {
    IN_STOCK("Aktiv"),
    LOW_STOCK("Azalan"),
    INACTIVE("Aktiv deyil");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public static ProductStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return INACTIVE;
        }

        String clean = status.trim().toLowerCase();

        if (clean.equals("aktiv") || clean.equals("active") || clean.equals("in_stock") || clean.equals("in stock") || clean.equals("stokda") || clean.equals("stokda var")) {
            return IN_STOCK;
        }
        if (clean.equals("azalan") || clean.equals("low_stock") || clean.equals("low stock") || clean.equals("az stok") || clean.equals("azalan stok")) {
            return LOW_STOCK;
        }
        if (clean.equals("aktiv deyil") || clean.equals("aktivdeyil") || clean.equals("deaktiv") || clean.equals("deactive") || clean.equals("inactive") || clean.equals("passiv") || clean.equals("passive")) {
            return INACTIVE;
        }

        for (ProductStatus s : values()) {
            if (s.name().equalsIgnoreCase(clean.replace(" ", "_")) || s.description.equalsIgnoreCase(status)) {
                return s;
            }
        }

        throw new BusinessException("Product status not recognized: " + status);
    }
}

