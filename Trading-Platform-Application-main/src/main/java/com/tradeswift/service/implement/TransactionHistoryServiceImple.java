package com.tradeswift.service.implement;

import com.tradeswift.models.User;
import com.tradeswift.models.Wallet;
import com.tradeswift.models.WalletTransaction;
import com.tradeswift.repositories.TransactionHistoryRepository;
import com.tradeswift.service.TransactionHistoryService;
import com.tradeswift.service.UserService;
import com.tradeswift.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class TransactionHistoryServiceImple implements TransactionHistoryService {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;


    @Override
    public List<WalletTransaction> getUserTransactionHistory(String jwt) throws Exception {
        User user = this.userService.findUserProfileByJwt(jwt);
        Wallet wallet = this.walletService.getUserWallet(user);
        
        // Add logging for debugging
        System.out.println("🔍 Looking up transactions for user ID: " + user.getId() + ", wallet ID: " + wallet.getId());
        
        // Get all transactions for debug purposes first
        List<WalletTransaction> allTransactions = this.transactionHistoryRepository.findAllTransactions();
        System.out.println("🔍 Total transactions in database: " + allTransactions.size());
        
        // Manual search to debug query issues
        int countedTransactions = 0;
        System.out.println("🔍 MANUAL TRANSACTION SEARCH 🔍");
        for (WalletTransaction tx : allTransactions) {
            if (tx.getWallet() == null) {
                System.out.println("🔍 Transaction " + tx.getId() + " has NULL wallet");
                continue;
            }
            
            if (tx.getWallet().getId() != null && tx.getWallet().getId().equals(wallet.getId())) {
                countedTransactions++;
                System.out.println("🔍 Found matching transaction: " + tx);
            } else {
                System.out.println("🔍 Transaction " + tx.getId() + " belongs to wallet " + tx.getWallet().getId());
            }
        }
        System.out.println("🔍 Manual count found " + countedTransactions + " transactions for wallet ID: " + wallet.getId());
        
        // Direct SQL count queries
        System.out.println("🔍 Running direct SQL count queries...");
        Long totalCount = this.transactionHistoryRepository.countAllTransactions();
        Long walletCount = this.transactionHistoryRepository.countTransactionsByWalletId(wallet.getId());
        System.out.println("🔍 SQL COUNT: Total transactions: " + totalCount + ", Wallet transactions: " + walletCount);
        
        // Try with direct SQL query first (most reliable)
        System.out.println("🔍 Trying direct SQL query...");
        List<WalletTransaction> directTransactions = this.transactionHistoryRepository.findDirectByWalletId(wallet.getId());
        System.out.println("🔍 Direct SQL query found " + directTransactions.size() + " transactions for wallet ID: " + wallet.getId());
        
        // Also try JPQL query for comparison
        System.out.println("🔍 Trying JPQL query...");
        List<WalletTransaction> transactions = this.transactionHistoryRepository.findByWalletId(wallet.getId());
        System.out.println("🔍 JPQL query found " + transactions.size() + " transactions for wallet ID: " + wallet.getId());
        
        // Use direct SQL result if it found transactions but JPQL didn't
        if (directTransactions.size() > 0 && transactions.size() == 0) {
            System.out.println("🔍 WARNING: Using direct SQL results because JPQL found 0 transactions!");
            transactions = directTransactions;
        }
        
        if (transactions.isEmpty() && countedTransactions > 0) {
            System.out.println("🔍 WARNING: Manual count found transactions but JPQL query returned none!");
            System.out.println("🔍 Returning manually filtered transactions as fallback");
            
            // Create filtered list as fallback
            List<WalletTransaction> filteredTransactions = new java.util.ArrayList<>();
            for (WalletTransaction tx : allTransactions) {
                if (tx.getWallet() != null && 
                    tx.getWallet().getId() != null && 
                    tx.getWallet().getId().equals(wallet.getId())) {
                    filteredTransactions.add(tx);
                }
            }
            
            return filteredTransactions;
        }
        
        return transactions;
    }
}
