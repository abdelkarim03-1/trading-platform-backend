package com.tradeswift.service;

import com.stripe.exception.StripeException;
import com.tradeswift.models.PaymentOrder;
import com.tradeswift.models.User;
import com.tradeswift.responce.PaymentResponce;

public interface PaymentOrderService {
    PaymentOrder createOrder(User user, Long amount);
    PaymentOrder getPaymentOrderById(Long id);
    Boolean proccedPaymentOrder(PaymentOrder paymentOrder, String paymentId);
    PaymentResponce createStripePaymentLink(User user, Long amount, Long orderId) throws StripeException;
}