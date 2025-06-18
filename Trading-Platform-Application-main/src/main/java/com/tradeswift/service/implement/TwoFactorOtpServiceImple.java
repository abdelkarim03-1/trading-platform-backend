package com.tradeswift.service.implement;

import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.TwoFactorOtp;
import com.tradeswift.models.User;
import com.tradeswift.repositories.TwoFactorOtpRepository;
import com.tradeswift.service.TwoFactorOtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TwoFactorOtpServiceImple implements TwoFactorOtpService {


    @Autowired
    private TwoFactorOtpRepository twoFactorOtpRepository;

    @Override
    public TwoFactorOtp createTwoFactorOtp(User user, String otp, String jwt) {

        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();

        TwoFactorOtp twoFactorOtp = new TwoFactorOtp();

        twoFactorOtp.setOtp(otp);
        twoFactorOtp.setJwt(jwt);
        twoFactorOtp.setUser(user);
        twoFactorOtp.setId(id);

        return this.twoFactorOtpRepository.save(twoFactorOtp);
    }

    @Override
    public TwoFactorOtp findByUserId(Long userId) {

        return this.twoFactorOtpRepository.findByUserId(userId);
    }

    @Override
    public TwoFactorOtp findById(String id) {
        return this.twoFactorOtpRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("TwoFactorOtp","id",String.valueOf(id)));
    }

    @Override
    public boolean verifyTwoFactorOtp(TwoFactorOtp twoFactorOtp, String otp) {
        return twoFactorOtp.getOtp().equals(otp);
    }

    @Override
    public void deleteTwoFactorOtp(TwoFactorOtp twoFactorOtp) {
        this.twoFactorOtpRepository.delete(twoFactorOtp);
    }
}
