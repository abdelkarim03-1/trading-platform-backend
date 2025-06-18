package com.tradeswift.service;

import com.tradeswift.domain.VerificationType;
import com.tradeswift.models.ForgetPassword;
import com.tradeswift.models.User;

public interface ForgetPasswordService {

    ForgetPassword createToken(User user, String id, String otp, VerificationType verificationType,String sendTo);
    ForgetPassword findById(String id);
    ForgetPassword findByUser(User user);
    void deleteToken(ForgetPassword token);
}
