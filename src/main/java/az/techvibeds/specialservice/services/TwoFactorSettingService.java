package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingRequest;
import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingResponse;

public interface TwoFactorSettingService {

    TwoFactorSettingResponse getSettings(String adminEmail);

    TwoFactorSettingResponse toggleTwoFactor(TwoFactorSettingRequest request, String adminEmail);
}
