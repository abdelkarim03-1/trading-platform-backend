package com.tradeswift.models;

import com.tradeswift.domain.VerificationType;
import lombok.Data;

@Data
public class TwoFactorAuth {
    private boolean isEnabled;
    private VerificationType verificationType;
}
