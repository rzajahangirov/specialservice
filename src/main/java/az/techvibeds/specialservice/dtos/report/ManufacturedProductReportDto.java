package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * İstehsal olunan mal (məhsul) hesabatı.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManufacturedProductReportDto {
    private Long orderId;
    private String productName;           // İstehsal olunan məhsulun adı
    private String productDescription;
    private Integer quantity;             // İstehsal olunan miqdar
    private String status;                // PLANNING, IN_PRODUCTION, COMPLETED, etc.
    private Integer progressPercentage;   // İstehsal faizi
    private Date startDate;
    private Date finishDate;
}
