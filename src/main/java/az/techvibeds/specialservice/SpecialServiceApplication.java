package az.techvibeds.specialservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpecialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpecialServiceApplication.class, args);
	}

}
