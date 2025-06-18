package com.tradeswift.models;
import com.tradeswift.domain.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    private WalletTransactionType walletTransactionType;
    private LocalDate date;

    private Long transferId;

    private String purpose;
    private long amount;
    
    // For debugging - toString method
    @Override
    public String toString() {
        return "WalletTransaction{" +
                "id=" + id +
                ", walletId=" + (wallet != null ? wallet.getId() : "null") +
                ", type=" + walletTransactionType +
                ", date=" + date +
                ", amount=" + amount +
                ", purpose='" + purpose + '\'' +
                '}';
    }
}
