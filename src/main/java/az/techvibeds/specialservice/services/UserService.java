package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.auth.UserAdminRegisterDto;
import az.techvibeds.specialservice.dtos.auth.UserRegisterDto;
import az.techvibeds.specialservice.dtos.auth.UserLoggedDto;
import az.techvibeds.specialservice.dtos.auth.UserResponseDto;
import az.techvibeds.specialservice.dtos.user.CompanyUserResponseDto;
import az.techvibeds.specialservice.dtos.user.UserDetailResponse;
import az.techvibeds.specialservice.dtos.user.UserListResponse;
import az.techvibeds.specialservice.dtos.user.UserUpdateDto;
import az.techvibeds.specialservice.dtos.user.SystemUserResponseDto;
import az.techvibeds.specialservice.models.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    User findByEmail(String name);

    UserLoggedDto getLoggedUserInfo(String email);

    UserResponseDto register(UserRegisterDto registerDto, String email);

    User findUserById(Long userId);


    void updateUserCompany(Long id, Long userId);

    User getUserByEmail(String email);

    UserResponseDto registerUserAdmin(@Valid UserAdminRegisterDto registerDto);

    List<CompanyUserResponseDto> getCompanyUsers(String name);

    UserResponseDto updateUser(Long id, @Valid UserUpdateDto updateDto, String email);

    void toggleUserStatus(Long id, @NotNull(message = "Status cannot be null") Boolean active, String email);

    void deleteUser(Long id, String email);

    Page<UserListResponse> getCompanyUsersForDashboard(String name, Pageable pageable);

    UserDetailResponse getUserDetails(Long id, String email);

    List<UserListResponse> getDeletedCompanyUsers(String name);

    void restoreUser(Long id);

    void resetUserPassword(Long id, @NotBlank(message = "New password cannot be empty") @Size(min = 6, message = "Password must be at least 6 characters long") String newPassword, String email);

    List<SystemUserResponseDto> getAllUsers();
}
