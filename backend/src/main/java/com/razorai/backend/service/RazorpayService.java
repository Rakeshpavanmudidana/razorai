package com.razorai.backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayService {

    private final String keyId;
    private final String keySecret;

    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret
    ) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public Order createRazorpayOrder(
            BigDecimal amount,
            String receipt
    ) throws Exception {

        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        long amountInPaise =
                amount.multiply(BigDecimal.valueOf(100))
                        .longValue();

        JSONObject options = new JSONObject();

        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put("receipt", receipt);

        return razorpayClient.orders.create(options);
    }

    public String getKeyId() {
        return keyId;
    }


    public boolean verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {

        try {

            org.json.JSONObject options =
                    new org.json.JSONObject();

            options.put(
                    "razorpay_order_id",
                    razorpayOrderId
            );

            options.put(
                    "razorpay_payment_id",
                    razorpayPaymentId
            );

            options.put(
                    "razorpay_signature",
                    razorpaySignature
            );

            return com.razorpay.Utils.verifyPaymentSignature(
                    options,
                    keySecret
            );

        } catch (Exception e) {

            return false;
        }
    }

    public com.razorpay.Refund createRefund(
            String razorpayPaymentId,
            BigDecimal amount
    ) throws Exception {

        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        long amountInPaise =
                amount.multiply(BigDecimal.valueOf(100))
                        .longValue();

        JSONObject options = new JSONObject();

        options.put("amount", amountInPaise);

        return razorpayClient.payments.refund(
                razorpayPaymentId,
                options
        );
    }
}