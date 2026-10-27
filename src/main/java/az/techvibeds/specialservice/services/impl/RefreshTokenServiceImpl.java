package az.techvibeds.specialservice.services.impl;


import az.techvibeds.specialservice.models.RefreshToken;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.RefreshTokenRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(8 * 60 * 60 * 1000));

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public boolean removeToken(String email) {
        try {
            User user = userRepository.findByEmail(email).orElseThrow();
            if (user != null){
                RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId()).orElseThrow();
                refreshTokenRepository.delete(refreshToken);
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }


    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if(token.getExpiryDate().compareTo(Instant.now())<0){
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token is expired. Please make a new login..!");
        }
        return token;
    }
}