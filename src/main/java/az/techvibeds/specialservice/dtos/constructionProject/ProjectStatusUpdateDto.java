package az.techvibeds.specialservice.dtos.constructionProject;

import az.techvibeds.specialservice.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectStatusUpdateDto {
    @NotNull(message = "Status cannot be null")
    private ProjectStatus status;
}
