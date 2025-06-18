package com.tradeswift.service.implement;
import com.tradeswift.domain.WithdrawStatus;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.User;
import com.tradeswift.models.Withdrawal;
import com.tradeswift.repositories.WithdrawalRepository;
import com.tradeswift.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WithdrawalServiceImple implements WithdrawalService {

    @Autowired
    private WithdrawalRepository withdrawalRepository;


    @Override
    public Withdrawal requestWithdrawal(Long amount, User user) {

        Withdrawal withdrawal = new Withdrawal();

        withdrawal.setAmount(amount);
        withdrawal.setUser(user);
        withdrawal.setWithdrawStatus(WithdrawStatus.PENDING);

        return this.withdrawalRepository.save(withdrawal);
    }

    @Override
    public Withdrawal procedWithwithdrawal(Long withdrawalId, boolean accept) {
        Withdrawal withdrawal = this.withdrawalRepository.findById(withdrawalId).orElseThrow(()->new ResourceNotFoundException("withdrawal","id",String.valueOf(withdrawalId)));

        if(accept){
            withdrawal.setWithdrawStatus(WithdrawStatus.SUCCESS);
        }
        else{
            withdrawal.setWithdrawStatus(WithdrawStatus.PENDING);
        }
        return this.withdrawalRepository.save(withdrawal);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {
        return this.withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return this.withdrawalRepository.findAll();
    }
}
