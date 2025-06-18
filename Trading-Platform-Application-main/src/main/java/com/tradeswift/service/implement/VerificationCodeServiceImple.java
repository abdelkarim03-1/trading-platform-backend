package com.tradeswift.service.implement;
import com.tradeswift.domain.VerificationType;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.User;
import com.tradeswift.models.VerificationCode;
import com.tradeswift.repositories.VerificationCodeRepository;
import com.tradeswift.service.VerificationCodeService;
import com.tradeswift.utils.OtpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeServiceImple implements VerificationCodeService {

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Override
    public VerificationCode sendVerificationCode(User user, VerificationType verificationType) {
        VerificationCode verificationCode = new VerificationCode();

        verificationCode.setVerificationType(verificationType);
        verificationCode.setUser(user);
        verificationCode.setOtp(String.valueOf(OtpUtils.generateOTP()));

        return this.verificationCodeRepository.save(verificationCode);
    }

    @Override
    public VerificationCode getVerificationCodeById(Long id) {
        return this.verificationCodeRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("verificationCode","id",String.valueOf(id)));
    }

    @Override
    public VerificationCode getVerificationCodeByUser(Long userId) {
        return this.verificationCodeRepository.findByUserId(userId);
    }

    @Override
    public void deleteVerificationCodeById(VerificationCode verificationCode) {
        this.verificationCodeRepository.delete(verificationCode);
    }
}
