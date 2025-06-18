package com.tradeswift.responce;

import lombok.Data;

@Data
public class AuthResponce {
    private String jwt;
    private String message;
    private boolean status;
    private boolean isTwoFactorAuthEnabled;
    private String session;
}
