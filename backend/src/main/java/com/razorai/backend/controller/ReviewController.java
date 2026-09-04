package com.razorai.backend.controller;

import com.razorai.backend.entity.Review;
import com.razorai.backend.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Review addReview(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer rating,
            @RequestParam String comment
    ) {

        return reviewService.addReview(
                userId,
                productId,
                rating,
                comment
        );
    }

    @GetMapping("/product/{productId}")
    public List<Review> getProductReviews(
            @PathVariable Long productId
    ) {

        return reviewService.getProductReviews(productId);
    }

    @GetMapping("/user/{userId}")
    public List<Review> getUserReviews(
            @PathVariable Long userId
    ) {

        return reviewService.getUserReviews(userId);
    }
}