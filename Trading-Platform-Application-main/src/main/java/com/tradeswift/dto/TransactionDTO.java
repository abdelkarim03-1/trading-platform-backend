package com.tradeswift.dto;

import com.tradeswift.domain.WalletTransactionType;
import com.tradeswift.models.WalletTransaction;

import java.time.LocalDate;

/**
 * Data Transfer Object for WalletTransaction
 * This ensures proper serialization of all fields, especially walletId
 */
public class TransactionDTO {
    private Long id;
    private Long walletId;
    private WalletTransactionType walletTransactionType;
    private LocalDate date;
    private Long transferId;
    private String purpose;
    private long amount;

    // Default constructor
    public TransactionDTO() {
    }

    // Constructor from WalletTransaction entity
    public TransactionDTO(WalletTransaction transaction) {
        this.id = transaction.getId();
        this.walletId = transaction.getWallet() != null ? transaction.getWallet().getId() : null;
        this.walletTransactionType = transaction.getWalletTransactionType();
        this.date = transaction.getDate();
        this.transferId = transaction.getTransferId();
        this.purpose = transaction.getPurpose();
        this.amount = transaction.getAmount();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public WalletTransactionType getWalletTransactionType() {
        return walletTransactionType;
    }

    public void setWalletTransactionType(WalletTransactionType walletTransactionType) {
        this.walletTransactionType = walletTransactionType;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "TransactionDTO{" +
                "id=" + id +
                ", walletId=" + walletId +
                ", type=" + walletTransactionType +
                ", date=" + date +
                ", amount=" + amount +
                ", purpose='" + purpose + '\'' +
                '}';
    }
}