package com.razorai.backend.controller;

import com.razorai.backend.entity.Cart;
import com.razorai.backend.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable Long userId) {

        return cartService.getOrCreateCart(userId);
    }

    @PostMapping("/{userId}/items")
    public Cart addProduct(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity
    ) {

        return cartService.addProductToCart(
                userId,
                productId,
                quantity
        );
    }

    @PutMapping("/items/{cartItemId}")
    public Cart updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity
    ) {

        return cartService.updateQuantity(
                cartItemId,
                quantity
        );
    }

    @DeleteMapping("/items/{cartItemId}")
    public String removeItem(
            @PathVariable Long cartItemId
    ) {

        cartService.removeCartItem(cartItemId);

        return "Item removed from cart";
    }
}