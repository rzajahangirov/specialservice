package az.techvibeds.specialservice.dtos.constructionMaterial;

import az.techvibeds.specialservice.enums.InvoiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ConstructionProjectMaterialUseDto {
    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotNull(message = "Warehouse is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Partner is required for invoice")
    private Long partnerId;

    private LocalDate useDate;
    private InvoiceType invoiceType = InvoiceType.SALES;
    private String note;
}
