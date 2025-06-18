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
 * Emergency controller to fix wallet transaction issues
 * This should be removed after fixing the problem
 */
@RestController
@RequestMapping("/api/fix")
public class EmergencyFixController {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get database table info
     */
    @GetMapping("/tables")
    public ResponseEntity<Map<String, Object>> getTableInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Object[]> tables = entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' " +
                "AND (table_name LIKE '%wallet%' OR table_name LIKE '%transaction%')"
            ).getResultList();
            
            result.put("tables", tables);
            
            // Check wallet_transaction columns
            List<Object[]> columns = entityManager.createNativeQuery(
                "SELECT column_name, data_type, is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'wallet_transaction'"
            ).getResultList();
            
            result.put("wallet_transaction_columns", columns);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Get wallet transactions directly from the database
     */
    @GetMapping("/wallet-transactions")
    public ResponseEntity<Map<String, Object>> getWalletTransactions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get all transactions
            List<WalletTransaction> transactions = transactionHistoryRepository.findAllTransactions();
            result.put("totalCount", transactions.size());
            
            // Get transactions with null wallet
            List<Object[]> nullWalletTxs = entityManager.createNativeQuery(
                "SELECT id, amount, date FROM wallet_transaction WHERE wallet_id IS NULL"
            ).getResultList();
            
            result.put("nullWalletCount", nullWalletTxs.size());
            result.put("nullWalletTransactions", nullWalletTxs);
            
            // Count by wallet
            List<Object[]> countByWallet = entityManager.createNativeQuery(
                "SELECT wallet_id, COUNT(*) FROM wallet_transaction GROUP BY wallet_id"
            ).getResultList();
            
            result.put("countByWallet", countByWallet);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Create test transactions for a wallet
     */
    @PostMapping("/create-test-transactions/{walletId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestTransactions(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "3") int count) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            for (int i = 0; i < count; i++) {
                WalletTransaction tx = new WalletTransaction();
                tx.setWallet(wallet);
                tx.setAmount(100L * (i + 1));
                tx.setDate(LocalDate.now());
                tx.setPurpose("Test transaction " + (i + 1));
                
                // Alternate between deposit and withdrawal
                if (i % 2 == 0) {
                    tx.setWalletTransactionType(WalletTransactionType.ADD_MONEY);
                } else {
                    tx.setWalletTransactionType(WalletTransactionType.WITHDRAWAL);
                }
                
                transactionHistoryRepository.save(tx);
            }
            
            // Verify transactions were created
            Long newCount = transactionHistoryRepository.countTransactionsByWalletId(walletId);
            
            result.put("status", "success");
            result.put("message", count + " test transactions created");
            result.put("newTransactionCount", newCount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Fix transactions with null wallet_id
     */
    @PostMapping("/fix-null-wallet/{walletId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> fixNullWallet(@PathVariable Long walletId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            // Find transactions with null wallet
            List<WalletTransaction> nullWalletTxs = entityManager.createQuery(
                "SELECT t FROM WalletTransaction t WHERE t.wallet IS NULL",
                WalletTransaction.class
            ).getResultList();
            
            // Fix each transaction
            for (WalletTransaction tx : nullWalletTxs) {
                tx.setWallet(wallet);
                transactionHistoryRepository.save(tx);
            }
            
            result.put("status", "success");
            result.put("message", nullWalletTxs.size() + " transactions fixed");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Fix direct in database with native SQL
     */
    @PostMapping("/fix-direct/{walletId}")
    public ResponseEntity<Map<String, Object>> fixDirect(@PathVariable Long walletId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Update directly with SQL
            int updated = entityManager.createNativeQuery(
                "UPDATE wallet_transaction SET wallet_id = :walletId WHERE wallet_id IS NULL"
            ).setParameter("walletId", walletId)
            .executeUpdate();
            
            result.put("status", "success");
            result.put("updatedCount", updated);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}