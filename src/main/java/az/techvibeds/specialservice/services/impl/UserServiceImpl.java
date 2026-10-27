package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.auth.UserAdminRegisterDto;
import az.techvibeds.specialservice.dtos.auth.UserRegisterDto;
import az.techvibeds.specialservice.dtos.auth.UserLoggedDto;
import az.techvibeds.specialservice.dtos.auth.UserResponseDto;
import az.techvibeds.specialservice.dtos.moduleRole.UserModulePermissionDto;
import az.techvibeds.specialservice.dtos.user.CompanyUserResponseDto;
import az.techvibeds.specialservice.dtos.user.UserDetailResponse;
import az.techvibeds.specialservice.dtos.user.UserListResponse;
import az.techvibeds.specialservice.dtos.user.UserUpdateDto;
import az.techvibeds.specialservice.dtos.user.SystemUserResponseDto;
import az.techvibeds.specialservice.enums.ModuleRole;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Role;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.CompanyRepository;
import az.techvibeds.specialservice.repositories.RoleRepository;
import az.techvibeds.specialservice.repositories.UserModuleRoleRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.repositories.ModuleRepository;
import az.techvibeds.specialservice.services.AuditLogService;
import az.techvibeds.specialservice.services.UserModuleRoleService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final UserModuleRoleService userModuleRoleService;
    private final UserModuleRoleRepository userModuleRoleRepository;
    private final AuditLogService auditLogService;
    private final ModuleRepository moduleRepository;

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public UserResponseDto register(UserRegisterDto registerDto, String adminEmail) {
        log.info("Registering employee {} by admin {}", registerDto.getEmail(), adminEmail);

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            log.warn("Registration failed: Email {} is already taken", registerDto.getEmail());
            throw new RuntimeException("This email is already registered!");
        }

        User currentUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Current admin not found: " + adminEmail));

        Company company = companyRepository.findByUsers_Id(currentUser.getId());

        User user = modelMapper.map(registerDto, User.class);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setCompany(company);

        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role 'USER' not found in database"));
        user.setRoles(List.of(defaultRole));

        User savedUser = userRepository.save(user);
        log.info("Employee registered successfully with ID: {}", savedUser.getId());

        return modelMapper.map(savedUser, UserResponseDto.class);
    }

    @Override
    public User findUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });
    }

    @Override
    @Transactional
    public void updateUserCompany(Long companyId, Long userId) {
        log.info("Updating company for user ID: {} to company ID: {}", userId, companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setCompany(company);
        userRepository.save(user);
        log.info("User company updated successfully");
    }

    @Override
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
    }

    @Override
    @Transactional
    public UserResponseDto registerUserAdmin(UserAdminRegisterDto registerDto) {
        log.info("Registering Company Admin for email: {}", registerDto.getEmail());

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("This email is already registered!");
        }

        Company company = companyRepository.findById(registerDto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", registerDto.getCompanyId()));

        User user = modelMapper.map(registerDto, User.class);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setCompany(company);

        Role adminRole = roleRepository.findByName("USER_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role 'USER_ADMIN' not found"));
        user.setRoles(List.of(adminRole));

        User savedUser = userRepository.save(user);

        log.info("Company Admin saved. Proceeding to assign all company modules...");
        userModuleRoleService.assignAllCompanyModulesToUser(savedUser, ModuleRole.SUPER_ADMIN);

        return modelMapper.map(savedUser, UserResponseDto.class);
    }

    @Override
    public List<CompanyUserResponseDto> getCompanyUsers(String adminEmail) {
        log.debug("Fetching all company users for admin: {}", adminEmail);
        User admin = findByEmail(adminEmail);
        List<User> companyUsers;
        
        if (admin.getCompany() == null) {
            // Super Admin
            companyUsers = userRepository.findAll();
        } else {
            Long companyId = admin.getCompany().getId();
            companyUsers = userRepository.findAllByCompanyId(companyId);
        }
        log.info("Found {} users", companyUsers.size());

        return companyUsers.stream().map(user -> {
            List<UserModulePermissionDto> permissions = userModuleRoleRepository.findByUserId(user.getId())
                    .stream()
                    .map(umr -> UserModulePermissionDto.builder()
                            .moduleName(umr.getCompanyModule().getModule().getName())
                            .role(umr.getRole().name())
                            .build())
                    .toList();

            return CompanyUserResponseDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .surname(user.getSurname())
                    .email(user.getEmail())
                    .permissions(permissions)
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateDto updateDto, String email) {
        log.info("Service: Updating user profile for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Update failed: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });
        User admin = findByEmail(email);
        if (admin.getCompany() != null && (user.getCompany() == null || !admin.getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("You are not allowed to update this company's personal");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("You are not allowed to update this company's personal");
        }

        if (!user.getEmail().equals(updateDto.getEmail()) && userRepository.existsByEmail(updateDto.getEmail())) {
            log.warn("Update failed: Email {} is already in use", updateDto.getEmail());
            throw new BusinessException("This email address is already registered.");
        }

        user.setName(updateDto.getName());
        user.setSurname(updateDto.getSurname());
        user.setEmail(updateDto.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("User ID: {} updated successfully", userId);

        auditLogService.logAction(email,
                "USER_UPDATE",
                "User ID: " + userId,
                "Profile updated for: " + user.getEmail());

        return modelMapper.map(updatedUser, UserResponseDto.class);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, Boolean active, String email) {
        log.info("Service: Toggling status for user ID: {} to {}", userId, active);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        User admin = findByEmail(email);
        if (admin.getCompany() != null && (user.getCompany() == null || !admin.getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("You are not allowed to update this company's personal");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("You are not allowed to update this company's personal");
        }

        user.setActive(active);
        auditLogService.logAction(email,
                "STATUS_CHANGE",
                "User ID: " + userId,
                "Status changed to: " + active);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, String email) {
        log.warn("Service: Executing soft delete for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User admin = findByEmail(email);
        if (admin.getCompany() != null && (user.getCompany() == null || !admin.getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("You are not allowed to delete this company's personal");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("You are not allowed to delete this company's personal");
        }

        user.setDeleted(true);
        user.setActive(false);

        userRepository.save(user);
        auditLogService.logAction(email,
                "USER_DELETE",
                "User ID: " + userId,
                "Soft delete executed");
        log.info("User ID: {} successfully soft deleted", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> getCompanyUsersForDashboard(String adminEmail, Pageable pageable) {
        log.info("Fetching paginated user list for admin: {}", adminEmail);

        User admin = findByEmail(adminEmail);
        
        if (admin.getCompany() == null) {
            // Super Admin
            return userRepository.findAll(pageable)
                    .map(user -> UserListResponse.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .surname(user.getSurname())
                            .email(user.getEmail())
                            .active(user.getActive())
                            .build());
        }
        
        Long companyId = admin.getCompany().getId();
 
        return userRepository.findAllByCompanyIdAndDeletedFalse(companyId, pageable)
                .map(user -> UserListResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .surname(user.getSurname())
                        .email(user.getEmail())
                        .active(user.getActive())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetails(Long userId, String email) {
        log.info("Fetching full details for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        User admin = findByEmail(email);
        if (admin.getCompany() != null && (user.getCompany() == null || !admin.getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("You cannot view details of a user from another company.");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("You cannot view details of a user from another company.");
        }

        List<UserModulePermissionDto> permissions = userModuleRoleRepository.findByUserId(user.getId())
                .stream()
                .map(umr -> UserModulePermissionDto.builder()
                        .moduleName(umr.getCompanyModule().getModule().getName())
                        .role(umr.getRole().name())
                        .build())
                .toList();

        return UserDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .active(user.getActive())
                .modulePermissions(permissions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserListResponse> getDeletedCompanyUsers(String adminEmail) {
        log.info("Fetching soft-deleted users for admin: {}", adminEmail);

        User admin = findByEmail(adminEmail);
        List<User> deletedUsers;
        
        if (admin.getCompany() == null) {
            // Super Admin sees all deleted users? 
            // The method name implies company users, but for Super Admin we return all.
            deletedUsers = userRepository.findAll().stream().filter(User::getDeleted).toList();
        } else {
            Long companyId = admin.getCompany().getId();
            deletedUsers = userRepository.findAllDeletedByCompanyId(companyId);
        }

        return deletedUsers.stream()
                .map(user -> UserListResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .surname(user.getSurname())
                        .email(user.getEmail())
                        .active(user.getActive())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void restoreUser(Long userId) {
        log.info("Restoring soft-deleted user with ID: {}", userId);

        userRepository.restoreUserById(userId);

        log.info("User ID: {} successfully restored", userId);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long userId, String newPassword, String email) {
        log.warn("Admin is resetting password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Password reset failed: User {} not found", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });
        User admin = findByEmail(email);
        if (admin.getCompany() != null && (user.getCompany() == null || !admin.getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("Your company doesn't have permission to reset password");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("Your company doesn't have permission to reset password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password for user {} has been successfully reset", user.getEmail());
        auditLogService.logAction(
                email,
                "PASSWORD_RESET",
                "User ID: " + userId,
                "Password was reset by administrator"
        );
    }

    @Override
    public UserLoggedDto getLoggedUserInfo(String email) {
        log.debug("Getting profile info for: {}", email);
        User user = findByEmail(email);
        UserLoggedDto userLoggedDto = modelMapper.map(user, UserLoggedDto.class);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        userLoggedDto.setRoles(roles);

        boolean isGlobalAdmin = roles.contains("ROLE_GLOBAL_ADMIN");
        List<UserModulePermissionDto> permissions = new java.util.ArrayList<>();
        if (isGlobalAdmin) {
            permissions.add(UserModulePermissionDto.builder()
                    .moduleName("Super_Global admin")
                    .role("SUPER_ADMIN")
                    .build());
        } else {
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
                    .collect(java.util.stream.Collectors.toList());
        }
        userLoggedDto.setPermissions(permissions);

        return userLoggedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserResponseDto> getAllUsers() {
        log.info("Fetching all users in the system for Global Admin");
        return userRepository.findAllWithCompanyAndRoles().stream().map(user -> {
            SystemUserResponseDto dto = SystemUserResponseDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .surname(user.getSurname())
                    .email(user.getEmail())
                    .active(user.getActive())
                    .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                    .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                    .roles(user.getRoles().stream().map(Role::getName).toList())
                    .build();
            return dto;
        }).toList();
    }
}