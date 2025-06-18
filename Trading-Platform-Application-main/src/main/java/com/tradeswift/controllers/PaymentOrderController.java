package com.tradeswift.controllers;

import com.stripe.exception.StripeException;
import com.tradeswift.models.PaymentOrder;
import com.tradeswift.models.User;
import com.tradeswift.responce.PaymentResponce;
import com.tradeswift.service.PaymentOrderService;
import com.tradeswift.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentOrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @PostMapping("/payment/amount/{amount}")
    public ResponseEntity<PaymentResponce> paymentHandler(
            @PathVariable Long amount,
            @RequestHeader("Authorization") String jwt
    ) throws StripeException {
        User user = this.userService.findUserProfileByJwt(jwt);

        // Create a payment order
        PaymentOrder order = this.paymentOrderService.createOrder(user, amount);

        // Create Stripe payment link
        PaymentResponce paymentResponce = this.paymentOrderService.createStripePaymentLink(user, amount, order.getId());

        return new ResponseEntity<>(paymentResponce, HttpStatus.CREATED);
    }
}