package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.user.SystemUserResponseDto;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<List<SystemUserResponseDto>> getAllUsers() {
        log.info("GET /api/v1/users - Fetching all users in the system (Global Admin access only)");
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
