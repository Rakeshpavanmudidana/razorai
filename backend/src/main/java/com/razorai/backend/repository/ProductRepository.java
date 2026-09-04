package com.razorai.backend.repository;

import com.razorai.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByPriceLessThanEqual(BigDecimal price);

    List<Product> findByNameContainingIgnoreCaseOrKeywordsContainingIgnoreCase(
            String name,
            String keywords
    );
}