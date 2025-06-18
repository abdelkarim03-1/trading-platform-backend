package com.tradeswift.service;
import com.tradeswift.models.Order;
import com.tradeswift.models.User;
import com.tradeswift.models.Wallet;

public interface WalletService {
    Wallet getUserWallet(User user) throws Exception;
    Wallet addBalance(Wallet wallet,Long amount);
    Wallet findWalletById(Long id) throws Exception;
    Wallet walletToWalletTransfer(User sender,Wallet receiverWaller,Long amount) throws Exception;
    Wallet payOrderPayment(Order order, User user) throws Exception;
}
