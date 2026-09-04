package com.razorai.backend.controller;

import com.razorai.backend.entity.Order;
import com.razorai.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ============================================================
    // CREATE ORDER
    // ============================================================

    @PostMapping("/{userId}")
    public Order createOrder(
            @PathVariable Long userId
    ) {
        return orderService.createOrder(userId);
    }

    // ============================================================
    // GET ORDER
    // ============================================================

    @GetMapping("/{id}")
    public Order getOrder(
            @PathVariable Long id
    ) {
        return orderService.getOrderById(id);
    }

    // ============================================================
    // GET USER ORDERS
    // ============================================================

    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(
            @PathVariable Long userId
    ) {
        return orderService.getOrdersByUser(userId);
    }

    // ============================================================
    // CANCEL PENDING ORDER
    // ============================================================

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long orderId
    ) {

        Order cancelledOrder =
                orderService.cancelOrder(orderId);

        return ResponseEntity.ok(cancelledOrder);
    }

    // ============================================================
    // CANCEL PAID ORDER + RAZORPAY REFUND
    // ============================================================

    @PutMapping("/{orderId}/cancel-paid")
    public ResponseEntity<Order> cancelPaidOrder(
            @PathVariable Long orderId
    ) {

        Order cancelledOrder =
                orderService.cancelPaidOrder(orderId);

        return ResponseEntity.ok(cancelledOrder);
    }


    @PutMapping("/{orderId}/delivery-status")
    public ResponseEntity<Order> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam String status
    ) {
        Order updatedOrder =
                orderService.updateDeliveryStatus(orderId, status);

        return ResponseEntity.ok(updatedOrder);
    }
}
