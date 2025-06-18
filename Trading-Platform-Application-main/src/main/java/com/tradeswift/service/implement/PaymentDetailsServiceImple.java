package com.tradeswift.service.implement;
import com.tradeswift.models.PaymentDetails;
import com.tradeswift.models.User;
import com.tradeswift.repositories.PaymentDetailsRepository;
import com.tradeswift.service.PaymentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentDetailsServiceImple implements PaymentDetailsService {

    @Autowired
    private PaymentDetailsRepository paymentDetailsRepository;

    @Override
    public PaymentDetails addPaymentDetails(String accountNumber, String accountHolderName, String ifsc, String bankName, User user) {

        PaymentDetails paymentDetails = new PaymentDetails();

        paymentDetails.setBankName(bankName);
        paymentDetails.setIfsc(ifsc);
        paymentDetails.setAccountHolderName(accountHolderName);
        paymentDetails.setAccountNumber(accountNumber);
        paymentDetails.setUser(user);

        return this.paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    public PaymentDetails getUserPaymentDetails(User user) {
        return this.paymentDetailsRepository.findByUserId(user.getId());
    }
}
