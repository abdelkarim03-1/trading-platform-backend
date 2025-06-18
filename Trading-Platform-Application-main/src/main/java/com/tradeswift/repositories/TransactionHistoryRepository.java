package com.tradeswift.repositories;

import com.tradeswift.models.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<WalletTransaction,Long> {

    // Using a custom JPQL query to be explicit about the join
    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet.id = :walletId")
    List<WalletTransaction> findByWalletId(@Param("walletId") Long walletId);
    
    // Method to find all transactions for debugging
    @Query("SELECT t FROM WalletTransaction t")
    List<WalletTransaction> findAllTransactions();
    
    // Direct SQL query to bypass potential JPA cache issues
    @Query(value = "SELECT * FROM wallet_transaction WHERE wallet_id = :walletId", nativeQuery = true)
    List<WalletTransaction> findDirectByWalletId(@Param("walletId") Long walletId);
    
    // Direct SQL count to check if transactions exist at all
    @Query(value = "SELECT COUNT(*) FROM wallet_transaction", nativeQuery = true)
    Long countAllTransactions();
    
    // Direct SQL count of transactions for a wallet
    @Query(value = "SELECT COUNT(*) FROM wallet_transaction WHERE wallet_id = :walletId", nativeQuery = true)
    Long countTransactionsByWalletId(@Param("walletId") Long walletId);

    List<WalletTransaction> findByPurposeContaining(String paymentId);
    // In TransactionHistoryRepository.java
    @Query("SELECT COUNT(t) > 0 FROM WalletTransaction t WHERE t.purpose LIKE %:paymentId%")
    boolean existsByPaymentId(@Param("paymentId") String paymentId);
}