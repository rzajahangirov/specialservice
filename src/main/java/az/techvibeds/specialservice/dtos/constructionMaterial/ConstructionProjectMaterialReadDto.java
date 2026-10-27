package az.techvibeds.specialservice.dtos.constructionMaterial;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ConstructionProjectMaterialReadDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private Integer requiredQuantity;
    private Integer usedQuantity;
    private Integer availableQuantity;
    private LocalDate lastUsedDate;
    private Long invoiceId;
    private String invoiceNumber;
    private String note;
}
