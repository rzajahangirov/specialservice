package az.techvibeds.specialservice.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @RequestMapping("/api/v1/")
    public String home() {
        return "Welcome to SpecialService API";
    }
}
