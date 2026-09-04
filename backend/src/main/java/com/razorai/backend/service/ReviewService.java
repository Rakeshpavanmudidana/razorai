package com.razorai.backend.service;

import com.razorai.backend.entity.Product;
import com.razorai.backend.entity.Review;
import com.razorai.backend.repository.ProductRepository;
import com.razorai.backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public Review addReview(
            Long userId,
            Long productId,
            Integer rating,
            String comment
    ) {

        // Validate rating
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException(
                    "Rating must be between 1 and 5"
            );
        }

        // Validate comment
        if (comment == null || comment.trim().isEmpty()) {
            throw new RuntimeException(
                    "Review comment cannot be empty"
            );
        }

        // Find product
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        // Create review
        Review review = new Review(
                userId,
                product,
                rating,
                comment.trim()
        );

        return reviewRepository.save(review);
    }

    public List<Review> getProductReviews(Long productId) {

        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<Review> getUserReviews(Long userId) {

        return reviewRepository.findByUserId(userId);
    }
}