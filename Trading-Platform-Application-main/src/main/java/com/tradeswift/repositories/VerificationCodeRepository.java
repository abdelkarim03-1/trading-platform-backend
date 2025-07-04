package com.tradeswift.repositories;

import com.tradeswift.models.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode,Long> {

    VerificationCode findByUserId(Long userId);
}
