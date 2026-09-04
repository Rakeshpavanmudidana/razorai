package com.razorai.backend.controller;

import com.razorai.backend.entity.Order;
import com.razorai.backend.entity.Payment;
import com.razorai.backend.repository.PaymentRepository;
import com.razorai.backend.service.CartService;
import com.razorai.backend.service.OrderService;
import com.razorai.backend.service.RazorpayService;
import org.springframework.web.bind.annotation.*;
import com.razorai.backend.service.CartService;

import java.util.HashMap;


import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final CartService cartService;

    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final PaymentRepository paymentRepository;

    public PaymentController(
            OrderService orderService,
            RazorpayService razorpayService,
            PaymentRepository paymentRepository,
            CartService cartService
    ) {
        this.orderService = orderService;
        this.razorpayService = razorpayService;
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
    }

    // ============================================================
    // CREATE RAZORPAY PAYMENT
    // ============================================================

    @PostMapping("/create/{orderId}")
    public Map<String, Object> createPayment(
            @PathVariable Long orderId
    ) throws Exception {

        Order order = orderService.getOrderById(orderId);

        // Order must be pending
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException(
                    "Order is not available for payment"
            );
        }

        // --------------------------------------------------------
        // Check whether a Payment record already exists
        // --------------------------------------------------------

        var existingPayment =
                paymentRepository.findByOrderId(orderId);

        if (existingPayment.isPresent()) {

            Payment payment = existingPayment.get();

            Map<String, Object> response =
                    new HashMap<>();

            response.put("orderId", order.getId());
            response.put("amount", order.getTotalAmount());
            response.put("currency", "INR");
            response.put(
                    "razorpayOrderId",
                    payment.getRazorpayOrderId()
            );
            response.put(
                    "razorpayKey",
                    razorpayService.getKeyId()
            );
            response.put(
                    "paymentStatus",
                    payment.getStatus()
            );
            response.put(
                    "message",
                    "Existing payment found"
            );

            return response;
        }

        // --------------------------------------------------------
        // Create new Razorpay order
        // --------------------------------------------------------

        com.razorpay.Order razorpayOrder =
                razorpayService.createRazorpayOrder(
                        order.getTotalAmount(),
                        "order_" + order.getId()
                );

        String razorpayOrderId =
                razorpayOrder.get("id");

        // --------------------------------------------------------
        // Save payment record
        // --------------------------------------------------------

        Payment payment = new Payment(
                order,
                razorpayOrderId,
                "CREATED"
        );

        paymentRepository.save(payment);

        // --------------------------------------------------------
        // Return payment information
        // --------------------------------------------------------

        Map<String, Object> response =
                new HashMap<>();

        response.put("orderId", order.getId());
        response.put("amount", order.getTotalAmount());
        response.put("currency", "INR");
        response.put("razorpayOrderId", razorpayOrderId);
        response.put(
                "razorpayKey",
                razorpayService.getKeyId()
        );
        response.put(
                "paymentStatus",
                "CREATED"
        );
        response.put(
                "message",
                "Payment created successfully"
        );

        return response;
    }


    // ============================================================
    // VERIFY PAYMENT
    // ============================================================

    @PostMapping("/verify")
    public Map<String, Object> verifyPayment(
            @RequestParam Long orderId,
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam String razorpaySignature
    ) {

        Map<String, Object> response =
                new HashMap<>();

        try {

            // ----------------------------------------------------
            // Get local order
            // ----------------------------------------------------

            Order order =
                    orderService.getOrderById(orderId);

            // ----------------------------------------------------
            // Get payment from our database
            // ----------------------------------------------------

            Payment payment =
                    paymentRepository
                            .findByOrderId(orderId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Payment record not found"
                                    )
                            );

            // ----------------------------------------------------
            // Use Razorpay Order ID stored in our database
            // ----------------------------------------------------

            String storedRazorpayOrderId =
                    payment.getRazorpayOrderId();

            // ----------------------------------------------------
            // Make sure frontend Razorpay Order ID matches
            // our database
            // ----------------------------------------------------

            if (!storedRazorpayOrderId.equals(
                    razorpayOrderId
            )) {

                throw new RuntimeException(
                        "Invalid Razorpay order ID"
                );
            }

            // ----------------------------------------------------
            // Verify Razorpay signature
            // ----------------------------------------------------

            boolean verified =
                    razorpayService.verifyPaymentSignature(
                            storedRazorpayOrderId,
                            razorpayPaymentId,
                            razorpaySignature
                    );

            // ----------------------------------------------------
            // PAYMENT VERIFIED
            // ----------------------------------------------------

            if (verified) {

                payment.setRazorpayPaymentId(
                        razorpayPaymentId
                );

                payment.setStatus("PAID");

                paymentRepository.save(payment);

                Order paidOrder =
                        orderService.markOrderAsPaid(orderId);

                cartService.clearCart(
                        paidOrder.getUserId()
                );

                response.put("success", true);
                response.put(
                        "message",
                        "Payment verified successfully"
                );
                response.put("orderId", orderId);
                response.put("status", "PAID");

            }

            // ----------------------------------------------------
            // PAYMENT VERIFICATION FAILED
            // ----------------------------------------------------

            else {

                payment.setStatus(
                        "VERIFICATION_FAILED"
                );

                paymentRepository.save(payment);

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Payment verification failed"
                );

                response.put(
                        "orderId",
                        orderId
                );

                response.put(
                        "status",
                        "VERIFICATION_FAILED"
                );
            }

        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            response.put(
                    "status",
                    "FAILED"
            );
        }

        return response;
    }
}
