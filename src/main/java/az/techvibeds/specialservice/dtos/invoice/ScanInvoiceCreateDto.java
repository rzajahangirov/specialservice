package az.techvibeds.specialservice.dtos.invoice;

import az.techvibeds.specialservice.enums.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanInvoiceCreateDto {

    private InvoiceType invoiceType; // SALES or PURCHASE (default to SALES if null)
    private Long partnerId;          // Optional
    private Long warehouseId;        // Optional
    private String notes;            // Optional

    @NotEmpty(message = "Skan edilmiş məhsulların siyahısı boş ola bilməz")
    @Valid
    private List<ScanBarcodeItemDto> items;
}
