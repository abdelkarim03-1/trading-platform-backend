package com.tradeswift.service;
import com.tradeswift.models.User;
import com.tradeswift.models.Withdrawal;
import java.util.List;

public interface WithdrawalService {
    Withdrawal requestWithdrawal(Long amount, User user);
    Withdrawal procedWithwithdrawal(Long withdrawalId,boolean accept);
    List<Withdrawal> getUsersWithdrawalHistory(User user);
    List<Withdrawal>getAllWithdrawalRequest();
}
