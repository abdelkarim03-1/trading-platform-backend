package com.tradeswift.service;
import com.tradeswift.models.PaymentDetails;
import com.tradeswift.models.User;

public interface PaymentDetailsService {
    public PaymentDetails addPaymentDetails(String accountNumber, String accountHolderName, String ifsc, String bankName, User user);
    public PaymentDetails getUserPaymentDetails(User user);
}
