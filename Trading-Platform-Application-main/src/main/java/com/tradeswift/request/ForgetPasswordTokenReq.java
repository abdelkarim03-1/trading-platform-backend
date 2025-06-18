package com.tradeswift.request;

import com.tradeswift.domain.VerificationType;
import lombok.Data;

@Data
public class ForgetPasswordTokenReq {
    private String sendTo;
    private VerificationType verificationType;
}
