package com.tradeswift.repositories;

import com.tradeswift.models.ForgetPassword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForgetPasswordRepository extends JpaRepository<ForgetPassword,String> {
    ForgetPassword findByUserId(Long userId);
}
