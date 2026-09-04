package com.razorai.backend.service;

import com.razorai.backend.entity.Cart;
import com.razorai.backend.entity.CartItem;
import com.razorai.backend.entity.Order;
import com.razorai.backend.entity.OrderItem;
import com.razorai.backend.entity.Payment;
import com.razorai.backend.repository.CartRepository;
import com.razorai.backend.repository.OrderRepository;
import com.razorai.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            PaymentRepository paymentRepository,
            RazorpayService razorpayService
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
    }

    // ============================================================
    // CREATE ORDER
    // ============================================================

    @Transactional
    public Order createOrder(Long userId) {

        Optional<Order> existingOrder =
                orderRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                "PENDING"
                        );

        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {

            BigDecimal itemTotal =
                    cartItem.getProduct()
                            .getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            cartItem.getQuantity()
                                    )
                            );

            total = total.add(itemTotal);
        }

        Order order = new Order(
                userId,
                total,
                "PENDING"
        );

        for (CartItem cartItem : cart.getItems()) {

            OrderItem orderItem = new OrderItem(
                    order,
                    cartItem.getProduct(),
                    cartItem.getQuantity(),
                    cartItem.getProduct().getPrice()
            );

            order.getItems().add(orderItem);
        }

        // Do NOT clear cart here.
        // Cart is cleared only after successful payment.

        return orderRepository.save(order);
    }

    // ============================================================
    // GET ORDER
    // ============================================================

    public Order getOrderById(Long id) {

        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));
    }

    // ============================================================
    // GET USER ORDERS
    // ============================================================

    public List<Order> getOrdersByUser(Long userId) {

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ============================================================
    // MARK ORDER AS PAID
    // ============================================================

    public Order markOrderAsPaid(Long orderId) {

        Order order = getOrderById(orderId);

        order.setStatus("PAID");

        return orderRepository.save(order);
    }

    // ============================================================
    // CANCEL PENDING ORDER
    // ============================================================

    public Order cancelOrder(Long orderId) {

        Order order = getOrderById(orderId);

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException(
                    "Only pending orders can be cancelled"
            );
        }

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }

    // ============================================================
    // CANCEL PAID ORDER + REFUND
    // ============================================================

    @Transactional
    public Order cancelPaidOrder(Long orderId) {

        Order order = getOrderById(orderId);

        // --------------------------------------------------------
        // Make sure order is paid
        // --------------------------------------------------------

        if (!"PAID".equals(order.getStatus())) {

            throw new RuntimeException(
                    "Only paid orders can be cancelled"
            );
        }

        // --------------------------------------------------------
        // Find payment record
        // --------------------------------------------------------

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment record not found"
                                )
                        );

        // --------------------------------------------------------
        // Make sure payment was actually successful
        // --------------------------------------------------------

        if (!"PAID".equals(payment.getStatus())) {

            throw new RuntimeException(
                    "Payment is not eligible for refund"
            );
        }

        // --------------------------------------------------------
        // Get Razorpay payment ID
        // --------------------------------------------------------

        String razorpayPaymentId =
                payment.getRazorpayPaymentId();

        if (razorpayPaymentId == null ||
                razorpayPaymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID not found"
            );
        }

        // --------------------------------------------------------
        // Create Razorpay refund
        // --------------------------------------------------------

        try {

            razorpayService.createRefund(
                    razorpayPaymentId,
                    order.getTotalAmount()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Refund failed: " + e.getMessage()
            );
        }

        // --------------------------------------------------------
        // Refund successful
        // --------------------------------------------------------

        payment.setStatus("REFUNDED");

        paymentRepository.save(payment);

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }

    // ============================================================
    // OLD REQUEST CANCEL METHOD
    // ============================================================
    // We no longer use CANCEL_REQUESTED.
    // Paid cancellation now directly performs the refund.


    public Order updateDeliveryStatus(Long orderId, String deliveryStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException(
                    "Only paid orders can have delivery status updated"
            );
        }

        if (!deliveryStatus.equals("PREPARING")
                && !deliveryStatus.equals("SHIPPED")
                && !deliveryStatus.equals("OUT_FOR_DELIVERY")
                && !deliveryStatus.equals("DELIVERED")) {

            throw new RuntimeException(
                    "Invalid delivery status"
            );
        }

        order.setDeliveryStatus(deliveryStatus);

        return orderRepository.save(order);
    }

}