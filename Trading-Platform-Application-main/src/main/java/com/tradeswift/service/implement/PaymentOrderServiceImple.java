package com.tradeswift.service.implement;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.tradeswift.domain.PaymentMethod;
import com.tradeswift.domain.PaymentOrderStatus;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.PaymentOrder;
import com.tradeswift.models.User;
import com.tradeswift.repositories.PaymentOrderRepository;
import com.tradeswift.responce.PaymentResponce;
import com.tradeswift.service.PaymentOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrderServiceImple implements PaymentOrderService {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Override
    public PaymentOrder createOrder(User user, Long amount) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentMethod(PaymentMethod.STRIPE);
        paymentOrder.setAmount(amount);
        paymentOrder.setUser(user);
        paymentOrder.setStatus(PaymentOrderStatus.PENDING);

        return this.paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) {
        return this.paymentOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "id", id + ""));
    }

    @Override
    public Boolean proccedPaymentOrder(PaymentOrder paymentOrder, String paymentId) {
        if (paymentOrder == null || paymentId == null) {
            return false;
        }

        // If already processed, return true but don't process again
        if (paymentOrder.getStatus().equals(PaymentOrderStatus.SUCCESS)) {
            return true;
        }

        if (paymentOrder.getStatus().equals(PaymentOrderStatus.PENDING)) {
            // Process Stripe payment confirmation here
            // For now, we'll just mark it as successful
            paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
            paymentOrderRepository.save(paymentOrder);
            return true;
        }
        return false;
    }

    @Override
    public PaymentResponce createStripePaymentLink(User user, Long amount, Long orderId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String successUrl = "http://localhost:5174/wallet?order_id=" + orderId + "&payment_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = "http://localhost:5174/payment/cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(amount * 100)
                                        .setProductData(SessionCreateParams
                                                .LineItem
                                                .PriceData
                                                .ProductData
                                                .builder()
                                                .setName("Top up wallet")
                                                .build()
                                        ).build()
                        ).build()
                ).build();

        Session session = Session.create(params);

        PaymentResponce res = new PaymentResponce();
        res.setPaymentUrl(session.getUrl());

        return res;
    }
}