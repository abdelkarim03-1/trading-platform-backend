package com.tradeswift.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for diagnosing database issues - creates a detailed report
 */
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generateReport() {
        Map<String, Object> report = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        
        try {
            // Check database schema
            report.put("database_tables", getDatabaseTables());
            
            // Check wallet_transaction table structure
            report.put("wallet_transaction_columns", getTableColumns("wallet_transaction"));
            
            // Check wallet table structure
            report.put("wallet_columns", getTableColumns("wallet"));
            
            // Check indexes
            report.put("wallet_transaction_indexes", getTableIndexes("wallet_transaction"));
            
            // Check transaction counts
            report.put("transaction_counts", getTransactionCounts());
            
            // Check sample transactions
            report.put("sample_transactions", getSampleTransactions());
            
            // Check for null wallet references
            report.put("null_wallet_transactions", getTransactionsWithNullWallet());
            
            // Check for orphaned transactions
            report.put("orphaned_transactions", getOrphanedTransactions());
            
            // Verify hibernate mappings
            report.put("mapping_verification", verifyEntityMappings());
            
        } catch (Exception e) {
            errors.put("error_message", e.getMessage());
            errors.put("stack_trace", e.getStackTrace());
            report.put("errors", errors);
        }
        
        return ResponseEntity.ok(report);
    }
    
    private List<Object[]> getDatabaseTables() {
        return entityManager.createNativeQuery(
            "SELECT table_name, table_type " +
            "FROM information_schema.tables " +
            "WHERE table_schema = 'public' " +
            "ORDER BY table_name"
        ).getResultList();
    }
    
    private List<Object[]> getTableColumns(String tableName) {
        return entityManager.createNativeQuery(
            "SELECT column_name, data_type, is_nullable, column_default " +
            "FROM information_schema.columns " +
            "WHERE table_name = ?1 " +
            "ORDER BY ordinal_position"
        ).setParameter(1, tableName).getResultList();
    }
    
    private List<Object[]> getTableIndexes(String tableName) {
        return entityManager.createNativeQuery(
            "SELECT indexname, indexdef " +
            "FROM pg_indexes " +
            "WHERE tablename = ?1"
        ).setParameter(1, tableName).getResultList();
    }
    
    private Map<String, Object> getTransactionCounts() {
        Map<String, Object> counts = new HashMap<>();
        
        // Total transactions
        Query totalQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM wallet_transaction");
        counts.put("total_transactions", ((Number) totalQuery.getSingleResult()).longValue());
        
        // Transactions by wallet
        List<Object[]> walletCounts = entityManager.createNativeQuery(
            "SELECT wallet_id, COUNT(*) " +
            "FROM wallet_transaction " +
            "GROUP BY wallet_id " +
            "ORDER BY COUNT(*) DESC"
        ).getResultList();
        counts.put("by_wallet", walletCounts);
        
        // Transactions by type
        List<Object[]> typeCounts = entityManager.createNativeQuery(
            "SELECT wallet_transaction_type, COUNT(*) " +
            "FROM wallet_transaction " +
            "GROUP BY wallet_transaction_type"
        ).getResultList();
        counts.put("by_type", typeCounts);
        
        return counts;
    }
    
    private List<Map<String, Object>> getSampleTransactions() {
        List<Object[]> results = entityManager.createNativeQuery(
            "SELECT id, wallet_id, wallet_transaction_type, amount, date, purpose " +
            "FROM wallet_transaction " +
            "ORDER BY id DESC " +
            "LIMIT 10"
        ).getResultList();
        
        List<Map<String, Object>> transactions = new java.util.ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> tx = new HashMap<>();
            tx.put("id", row[0]);
            tx.put("wallet_id", row[1]);
            tx.put("type", row[2]);
            tx.put("amount", row[3]);
            tx.put("date", row[4]);
            tx.put("purpose", row[5]);
            transactions.add(tx);
        }
        
        return transactions;
    }
    
    private List<Object[]> getTransactionsWithNullWallet() {
        return entityManager.createNativeQuery(
            "SELECT id, wallet_transaction_type, amount, date, purpose " +
            "FROM wallet_transaction " +
            "WHERE wallet_id IS NULL"
        ).getResultList();
    }
    
    private List<Object[]> getOrphanedTransactions() {
        return entityManager.createNativeQuery(
            "SELECT t.id, t.wallet_transaction_type, t.amount, t.date " +
            "FROM wallet_transaction t " +
            "LEFT JOIN wallet w ON t.wallet_id = w.id " +
            "WHERE w.id IS NULL AND t.wallet_id IS NOT NULL"
        ).getResultList();
    }
    
    private Map<String, Object> verifyEntityMappings() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check entity mapping for WalletTransaction
            String txTable = (String) entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_name = 'wallet_transaction'")
                .getSingleResult();
            result.put("wallet_transaction_table_exists", txTable != null);
            
            // Check entity mapping for Wallet
            String walletTable = (String) entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_name = 'wallet'")
                .getSingleResult();
            result.put("wallet_table_exists", walletTable != null);
            
            // Check join column
            List<Object> joinColumn = entityManager.createNativeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'wallet_transaction' AND column_name = 'wallet_id'")
                .getResultList();
            result.put("wallet_id_join_column_exists", !joinColumn.isEmpty());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}