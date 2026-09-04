package com.razorai.backend.repository;

import com.razorai.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    Optional<Order> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            String status
    );

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

}