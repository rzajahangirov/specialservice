package az.techvibeds.specialservice.controllers.auth;

import az.techvibeds.specialservice.dtos.auth.*;
import az.techvibeds.specialservice.dtos.moduleRole.UserModulePermissionDto;
import az.techvibeds.specialservice.models.RefreshToken;
import az.techvibeds.specialservice.models.Role;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.UserModuleRoleRepository;
import az.techvibeds.specialservice.security.JwtService;
import az.techvibeds.specialservice.services.RefreshTokenService;
import az.techvibeds.specialservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserModuleRoleRepository userModuleRoleRepository;
    private final az.techvibeds.specialservice.repositories.ModuleRepository moduleRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserLoggedDto> me(Principal principal) {
        log.info("Fetching logged in user info for: {}", principal.getName());
        UserLoggedDto userLoggedDto = userService.getLoggedUserInfo(principal.getName());
        return ResponseEntity.ok(userLoggedDto);
    }

    @PostMapping("/user/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegisterDto registerDto, Principal principal) {
        log.info("Public registration request for email: {}", registerDto.getEmail());
        // Əgər giriş edilməyibsə, null göndəririk ki, service özü şirkət tapsın
        String registrarEmail = (principal != null) ? principal.getName() : null;
        UserResponseDto response = userService.register(registerDto, registrarEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserAdminRegisterDto registerDto) {
        log.info("GLOBAL_ADMIN is registering a new Company Admin for company ID: {}", registerDto.getCompanyId());
        UserResponseDto response = userService.registerUserAdmin(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public JwtResponseDto login(@RequestBody LoginDto loginDto) {
        try {
            log.info("Attempting login for user: {}", loginDto.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                log.error("Authentication failed for user: {}", loginDto.getEmail());
                throw new UsernameNotFoundException("Invalid credentials provided.");
            }

            User user = userService.getUserByEmail(loginDto.getEmail());
            log.info("User found: {}, Company: {}", user.getEmail(), user.getCompany() != null ? user.getCompany().getId() : "NULL");

            List<String> authorities = user.getRoles().stream()
                    .map(Role::getName)
                    .toList();

            boolean isGlobalAdmin = authorities.contains("ROLE_GLOBAL_ADMIN");

            List<UserModulePermissionDto> permissions;
            if(isGlobalAdmin) {
                log.info("Populating Super_Global admin permission for GLOBAL_ADMIN: {}", user.getEmail());
                permissions = new java.util.ArrayList<>();
                permissions.add(UserModulePermissionDto.builder()
                        .moduleName("Super_Global admin")
                        .role("SUPER_ADMIN")
                        .build());
            }else{
                permissions = userModuleRoleRepository.findByUserId(user.getId())
                        .stream()
                        .map(umr -> {
                            String moduleName = "UNKNOWN";
                            if (umr.getCompanyModule() != null && umr.getCompanyModule().getModule() != null) {
                                moduleName = umr.getCompanyModule().getModule().getName();
                            }
                            return UserModulePermissionDto.builder()
                                    .moduleName(moduleName)
                                    .role(umr.getRole() != null ? umr.getRole().name() : "USER")
                                    .build();
                        })
                        .toList();
            }

            String accessToken = jwtService.generateToken(user, authorities, null);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

            log.info("Login successful for user: {}", loginDto.getEmail());
            return JwtResponseDto.builder()
                    .accessToken(accessToken)
                    .token(refreshToken.getToken())
                    .permissions(permissions)
                    .build();
        } catch (Exception e) {
            log.error("CRITICAL LOGIN ERROR: ", e);
            throw e;
        }
    }

    @PostMapping("/refreshToken")
    public JwtResponseDto refreshToken(@RequestBody RefreshTokenRequestDto refreshTokenRequestDTO) {
        log.info("Access token refresh requested");
        return refreshTokenService.findByToken(refreshTokenRequestDTO.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    List<String> authorities = user.getRoles().stream()
                            .map(Role::getName)
                            .toList();

                    String accessToken = jwtService.generateToken(user, authorities, null);
                    return JwtResponseDto.builder()
                            .accessToken(accessToken)
                            .token(refreshTokenRequestDTO.getToken())
                            .build();
                })
                .orElseThrow(() -> {
                    log.error("Refresh token rotation failed: Token not found in database");
                    return new RuntimeException("Refresh Token is not in database!");
                });
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(Principal principal) {
        log.info("User {} is logging out", principal.getName());
        refreshTokenService.removeToken(principal.getName());
        return ResponseEntity.ok().build();
    }
}