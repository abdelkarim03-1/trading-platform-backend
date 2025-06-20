package com.tradeswift.service.implement;
import com.tradeswift.domain.OrderType;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.Order;
import com.tradeswift.models.User;
import com.tradeswift.models.Wallet;
import com.tradeswift.repositories.WalletRepository;
import com.tradeswift.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletServiceImple implements WalletService {

    @Autowired
    private WalletRepository walletRepository;


    @Override
    public Wallet getUserWallet(User user) throws Exception {
        Wallet wallet = this.walletRepository.findByUserId(user.getId());
        if(wallet==null){
            wallet = new Wallet();
            wallet.setUser(user);
            this.walletRepository.save(wallet);
        }
        return wallet;
    }

    @Override
    public Wallet addBalance(Wallet wallet, Long amount) {

        BigDecimal oldBlance = wallet.getBalance();
        BigDecimal newBlance = oldBlance.add(BigDecimal.valueOf(amount));
        wallet.setBalance(newBlance);
        return this.walletRepository.save(wallet);
    }

    @Override
    public Wallet findWalletById(Long id) throws Exception {
        return this.walletRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("wallet","id",String.valueOf(id)));
    }

    @Override
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount) throws Exception {

        Wallet senderWallet  = getUserWallet(sender);

        if(senderWallet.getBalance().compareTo(BigDecimal.valueOf(amount))<0){
            throw new Exception("Insufficient Amount");
        }
        BigDecimal senderBalance = senderWallet.getBalance().subtract(BigDecimal.valueOf((amount)));
        senderWallet.setBalance(senderBalance);

        this.walletRepository.save(senderWallet);

        BigDecimal receiverBalance = receiverWallet.getBalance().add(BigDecimal.valueOf(amount));
        receiverWallet.setBalance(receiverBalance);
        this.walletRepository.save(receiverWallet);

        return senderWallet;
    }

    @Override
    public Wallet payOrderPayment(Order order, User user) throws Exception {

        Wallet wallet = getUserWallet(user);

        if(order.getOrderType().equals(OrderType.BUY)){
            BigDecimal newBalance = wallet.getBalance().subtract(order.getPrice());

            if(newBalance.compareTo(order.getPrice())<0){
                throw new Exception("Insufficient Balance for this transaction");
            }
            wallet.setBalance(newBalance);
        }else{
            BigDecimal newBalance = wallet.getBalance().add(order.getPrice());
            wallet.setBalance(newBalance);
        }
        return this.walletRepository.save(wallet);
    }
}
