package com.tradeswift.service;

import com.tradeswift.models.WalletTransaction;

import java.util.List;

public interface TransactionHistoryService {

    List<WalletTransaction> getUserTransactionHistory(String jwt) throws Exception;
}
