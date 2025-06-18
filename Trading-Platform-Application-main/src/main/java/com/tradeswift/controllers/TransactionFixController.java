package com.tradeswift.controllers;

import com.tradeswift.domain.WalletTransactionType;
import com.tradeswift.models.Wallet;
import com.tradeswift.models.WalletTransaction;
import com.tradeswift.repositories.TransactionHistoryRepository;
import com.tradeswift.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quick-fix controller for wallet transaction issues
 */
@RestController
@RequestMapping("/api/quickfix")
public class TransactionFixController {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create demo transactions for a wallet
     */
    @PostMapping("/create-demo/{walletId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> createDemoTransactions(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "3") int count) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the wallet
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));
            
            // Create sample transactions
            for (int i = 0; i < count; i++) {
                WalletTransaction tx = new WalletTransaction();
                tx.setWallet(wallet);
                tx.setAmount(1000L + i * 500);
                tx.setDate(LocalDate.now().minusDays(i));
                
                if (i % 2 == 0) {
                    tx.setWalletTransactionType(WalletTransactionType.ADD_MONEY);
                    tx.setPurpose("Demo deposit #" + (i+1));
                } else {
                    tx.setWalletTransactionType(WalletTransactionType.WITHDRAWAL);
                    tx.setPurpose("Demo withdrawal #" + (i+1));
                }
                
                // Save the transaction
                transactionHistoryRepository.save(tx);
            }
            
            // Verify transactions were added
            List<WalletTransaction> transactions = transactionHistoryRepository.findByWalletId(walletId);
            
            result.put("status", "success");
            result.put("message", count + " demo transactions created");
            result.put("transaction_count", transactions.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * Force direct SQL query to get transactions
     */
    @GetMapping("/transactions/{walletId}")
    public ResponseEntity<List<Map<String, Object>>> getTransactionsByWalletId(@PathVariable Long walletId) {
        List<Object[]> results = entityManager.createNativeQuery(
            "SELECT id, amount, date, purpose, wallet_transaction_type " +
            "FROM wallet_transaction " +
            "WHERE wallet_id = ?1 " +
            "ORDER BY date DESC"
        ).setParameter(1, walletId).getResultList();
        
        List<Map<String, Object>> transactions = new java.util.ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> tx = new HashMap<>();
            tx.put("id", row[0]);
            tx.put("amount", row[1]);
            tx.put("date", row[2]);
            tx.put("purpose", row[3]);
            tx.put("walletTransactionType", row[4]);
            transactions.add(tx);
        }
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Fix missing wallet IDs in transactions
     */
    @PostMapping("/fix-wallet-ids/{walletId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> fixWalletIds(@PathVariable Long walletId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the wallet
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));
            
            // Fix missing wallet IDs with direct SQL
            int updated = entityManager.createNativeQuery(
                "UPDATE wallet_transaction SET wallet_id = ?1 WHERE wallet_id IS NULL"
            ).setParameter(1, walletId).executeUpdate();
            
            result.put("status", "success");
            result.put("message", updated + " transactions updated with wallet ID");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * Get wallet info
     */
    @GetMapping("/wallet-info/{userId}")
    public ResponseEntity<Map<String, Object>> getWalletInfo(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get wallet by user ID
            Object[] walletInfo = (Object[]) entityManager.createNativeQuery(
                "SELECT w.id, w.balance FROM wallet w " +
                "JOIN users u ON w.user_id = u.id " +
                "WHERE u.id = ?1"
            ).setParameter(1, userId).getSingleResult();
            
            if (walletInfo != null) {
                result.put("wallet_id", walletInfo[0]);
                result.put("balance", walletInfo[1]);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}