package az.techvibeds.specialservice.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT) // Ancaq tam eyni adlar
                .setFieldMatchingEnabled(true)                 // Sahələrin birbaşa tutuşdurulması
                .setSkipNullEnabled(true)                      // Null dəyərləri kopyalama (ehtiyat üçün)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        return modelMapper;
    }
}