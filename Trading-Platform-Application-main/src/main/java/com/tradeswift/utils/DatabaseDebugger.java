package com.tradeswift.utils;

import com.tradeswift.models.Wallet;
import com.tradeswift.models.WalletTransaction;
import com.tradeswift.repositories.TransactionHistoryRepository;
import com.tradeswift.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Utility class for debugging and fixing database issues
 */
@Component
public class DatabaseDebugger {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Debug transaction-wallet relationships
     */
    public void debugTransactionWalletRelationships() {
        List<WalletTransaction> allTransactions = transactionHistoryRepository.findAllTransactions();
        System.out.println("===== DATABASE DEBUGGER =====");
        System.out.println("Total transactions: " + allTransactions.size());

        int nullWalletCount = 0;
        for (WalletTransaction tx : allTransactions) {
            if (tx.getWallet() == null) {
                nullWalletCount++;
                System.out.println("Transaction ID " + tx.getId() + " has NULL wallet");
            }
        }
        
        System.out.println("Transactions with NULL wallet: " + nullWalletCount);
        
        // Check if wallet_transaction table has wallet_id column
        try {
            Long count = (Long) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'wallet_transaction' AND column_name = 'wallet_id'"
            ).getSingleResult();
            
            System.out.println("wallet_id column exists in wallet_transaction table: " + (count > 0));
            
            // Check if wallet_id is nullable
            if (count > 0) {
                String nullable = (String) entityManager.createNativeQuery(
                    "SELECT is_nullable FROM information_schema.columns " +
                    "WHERE table_name = 'wallet_transaction' AND column_name = 'wallet_id'"
                ).getSingleResult();
                
                System.out.println("wallet_id column is nullable: " + "YES".equals(nullable));
            }
        } catch (Exception e) {
            System.out.println("Error checking schema: " + e.getMessage());
        }
    }

    /**
     * Fix transactions with missing wallet relationship
     */
    @Transactional
    public void fixMissingWalletReferences() {
        List<Object[]> orphanedTransactions = entityManager.createNativeQuery(
            "SELECT t.id, t.wallet_id FROM wallet_transaction t " +
            "LEFT JOIN wallet w ON t.wallet_id = w.id " +
            "WHERE t.wallet_id IS NULL"
        ).getResultList();
        
        System.out.println("Found " + orphanedTransactions.size() + " transactions with NULL wallet_id");
        
        // Get default wallet to assign transactions to
        List<Wallet> wallets = walletRepository.findAll();
        if (wallets.isEmpty()) {
            System.out.println("No wallets found - cannot fix orphaned transactions");
            return;
        }
        
        Wallet defaultWallet = wallets.get(0);
        System.out.println("Using wallet ID " + defaultWallet.getId() + " as default wallet");
        
        // Fix orphaned transactions
        for (Object[] row : orphanedTransactions) {
            Long txId = ((Number) row[0]).longValue();
            WalletTransaction tx = transactionHistoryRepository.findById(txId).orElse(null);
            
            if (tx != null) {
                tx.setWallet(defaultWallet);
                transactionHistoryRepository.save(tx);
                System.out.println("Fixed transaction ID " + txId + " - assigned to wallet ID " + defaultWallet.getId());
            }
        }
    }
}