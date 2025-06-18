package com.tradeswift.service;

import com.tradeswift.domain.VerificationType;
import com.tradeswift.models.User;

public interface UserService {
    User findByUserEmail(String email);
    User saveUser(User user);
    User findUserProfileByJwt(String jwt);
    User findById(Long userId);
    User enableTwoFactorAuthentication(VerificationType verificationType,String sendTo,User user);
    void updatePassword(User user, String password);
}
