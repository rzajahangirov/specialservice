package az.techvibeds.specialservice.dtos.invoice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanBarcodeItemDto {

    @NotBlank(message = "Barkod boş ola bilməz")
    private String barcode;

    @Min(value = 1, message = "Miqdar ən azı 1 olmalıdır")
    private Integer quantity = 1;
}
