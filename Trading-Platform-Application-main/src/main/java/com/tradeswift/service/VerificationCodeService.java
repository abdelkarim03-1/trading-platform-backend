package com.tradeswift.service;
import com.tradeswift.domain.VerificationType;
import com.tradeswift.models.User;
import com.tradeswift.models.VerificationCode;

public interface VerificationCodeService {
    VerificationCode sendVerificationCode(User user, VerificationType verificationType);
    VerificationCode getVerificationCodeById(Long id);
    VerificationCode getVerificationCodeByUser(Long userId);
    void deleteVerificationCodeById(VerificationCode verificationCode);
}
