package az.techvibeds.specialservice.dtos.mainpanel;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainPanelDto {
    private Map<String, Integer> categoryAndCount;

}
