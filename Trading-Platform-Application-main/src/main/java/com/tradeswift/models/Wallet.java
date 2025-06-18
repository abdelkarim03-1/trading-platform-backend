package com.tradeswift.models;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    private User user;

    private BigDecimal balance=BigDecimal.valueOf(0);
    
    // For debugging - toString method
    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : "null") +
                ", balance=" + balance +
                '}';
    }
}
