package com.tradeswift.controllers;

import com.tradeswift.models.PaymentDetails;
import com.tradeswift.models.User;
import com.tradeswift.service.PaymentDetailsService;
import com.tradeswift.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RestController
public class PaymentDetailsController {
    @Autowired
    private PaymentDetailsService paymentDetailsService;

    @Autowired
    private UserService userService;

    @PostMapping("/api/payment-details")
    public ResponseEntity<PaymentDetails> addPaymentDetails(@RequestBody PaymentDetails paymentDetailsReq,
                                                            @RequestHeader("Authorization") String jwt){

        User user = this.userService.findUserProfileByJwt(jwt);
        PaymentDetails paymentDetails = this.paymentDetailsService.addPaymentDetails(
                paymentDetailsReq.getAccountNumber(),
                paymentDetailsReq.getAccountHolderName(),
                paymentDetailsReq.getIfsc(),
                paymentDetailsReq.getBankName(),
                user
        );

        return new ResponseEntity<>(paymentDetails, HttpStatus.CREATED);
    }
    @GetMapping("/api/payment-details")
    public ResponseEntity<PaymentDetails>getUserPaymentDetails(@RequestHeader("Authorization") String jwt){
        User user = this.userService.findUserProfileByJwt(jwt);
        PaymentDetails paymentDetails = this.paymentDetailsService.getUserPaymentDetails(user);
        return new ResponseEntity<>(paymentDetails,HttpStatus.OK);
    }
}
