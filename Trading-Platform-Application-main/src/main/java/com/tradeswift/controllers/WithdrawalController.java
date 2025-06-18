package com.tradeswift.controllers;

import com.tradeswift.models.User;
import com.tradeswift.models.Wallet;
import com.tradeswift.models.Withdrawal;
import com.tradeswift.service.UserService;
import com.tradeswift.service.WalletService;
import com.tradeswift.service.WithdrawalService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WithdrawalController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private UserService userService;


    @Autowired
    private com.tradeswift.repositories.TransactionHistoryRepository transactionHistoryRepository;

    @PostMapping("/api/withdrawal/{amount}")
    @Transactional
    public ResponseEntity<?> withdrawalRequest(@PathVariable Long amount, @RequestHeader("Authorization") String jwt) throws Exception {
        User user = this.userService.findUserProfileByJwt(jwt);
        Wallet userWallet = this.walletService.getUserWallet(user);
        
        System.out.println("WITHDRAWAL REQUEST: User ID=" + user.getId() + ", Wallet ID=" + userWallet.getId());

        Withdrawal withdrawal = this.withdrawalService.requestWithdrawal(amount,user);
        this.walletService.addBalance(userWallet,-withdrawal.getAmount());
        
        // Create transaction record for withdrawal
        com.tradeswift.models.WalletTransaction walletTransaction = new com.tradeswift.models.WalletTransaction();
        walletTransaction.setWallet(userWallet);
        walletTransaction.setAmount(withdrawal.getAmount());
        walletTransaction.setDate(java.time.LocalDate.now());
        walletTransaction.setWalletTransactionType(com.tradeswift.domain.WalletTransactionType.WITHDRAWAL);
        walletTransaction.setPurpose("Withdrawal request");
        
        // Print transaction details before saving
        System.out.println("BEFORE SAVE: Transaction details: " + walletTransaction);
        
        // Save and log the transaction
        com.tradeswift.models.WalletTransaction savedTransaction = this.transactionHistoryRepository.save(walletTransaction);
        System.out.println("AFTER SAVE: Transaction details: " + savedTransaction);
        
        // Verify all transactions after this one
        java.util.List<com.tradeswift.models.WalletTransaction> allTransactions = 
            this.transactionHistoryRepository.findAllTransactions();
        System.out.println("Now there are " + allTransactions.size() + " total transactions in database");
        
        // Emergency direct database dump
        System.out.println("🔍 EMERGENCY DATABASE DUMP 🔍");
        System.out.println("Total transactions in database: " + allTransactions.size());
        System.out.println("Your wallet ID is: " + userWallet.getId());
        
        // Count transactions for this wallet
        int walletTransactionCount = 0;
        for (com.tradeswift.models.WalletTransaction tx : allTransactions) {
            if (tx.getWallet() != null && tx.getWallet().getId().equals(userWallet.getId())) {
                walletTransactionCount++;
                System.out.println("MATCH: " + tx);
            } else {
                if (tx.getWallet() == null) {
                    System.out.println("BAD TX: Null wallet - " + tx.getId());
                } else {
                    System.out.println("OTHER WALLET TX: " + tx.getWallet().getId() + " - " + tx);
                }
            }
        }
        System.out.println("Found " + walletTransactionCount + " transactions for wallet ID " + userWallet.getId());
        
        // Print last 3 transactions
        int start = Math.max(0, allTransactions.size() - 3);
        for (int i = start; i < allTransactions.size(); i++) {
            System.out.println("Transaction[" + i + "]: " + allTransactions.get(i));
        }

        return new ResponseEntity<>(withdrawal, HttpStatus.OK);
    }
    @PatchMapping("/api/admin/withdrawal/{id}/proceed/{accept}")
    public ResponseEntity<?>proceeWithdrawal(@PathVariable Long id,@PathVariable boolean accept,
                                             @RequestHeader("Authorization") String jwt) throws Exception {
        User user = this.userService.findUserProfileByJwt(jwt);
        Withdrawal withdrawal = this.withdrawalService.procedWithwithdrawal(id,accept);


        Wallet userWallet = this.walletService.getUserWallet(user);
        if(!accept){
            this.walletService.addBalance(userWallet,withdrawal.getAmount());
        }
        return  new ResponseEntity<>(withdrawal,HttpStatus.OK);
    }
    @GetMapping("/api/withdrawal")
    public ResponseEntity<List<Withdrawal>>getWithdrawalHistory(@RequestHeader("Authorization") String jwt){
        User user = this.userService.findUserProfileByJwt(jwt);
        List<Withdrawal>withdrawalHistory = this.withdrawalService.getUsersWithdrawalHistory(user);
        return new ResponseEntity<>(withdrawalHistory,HttpStatus.OK);
    }

    @GetMapping("/api/admin/withdrawal")
    public ResponseEntity<List<Withdrawal>>getAllWithdrawalRequest(@RequestHeader("Authorization") String jwt){
        User user = this.userService.findUserProfileByJwt(jwt);
        List<Withdrawal>withdrawalRequestList = this.withdrawalService.getAllWithdrawalRequest();
        return new ResponseEntity<>(withdrawalRequestList,HttpStatus.OK);
    }
}
