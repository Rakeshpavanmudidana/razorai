package com.razorai.backend.service;

import com.razorai.backend.entity.Cart;
import com.razorai.backend.entity.CartItem;
import com.razorai.backend.entity.Product;
import com.razorai.backend.repository.CartItemRepository;
import com.razorai.backend.repository.CartRepository;
import com.razorai.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public Cart getOrCreateCart(Long userId) {

        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    public Cart addProductToCart(
            Long userId,
            Long productId,
            Integer quantity
    ) {

        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        if (product.getStock() < quantity) {
            throw new RuntimeException("Not enough stock");
        }

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                productId
                        )
                        .orElse(null);

        if (cartItem != null) {

            int newQuantity =
                    cartItem.getQuantity() + quantity;

            if (newQuantity > product.getStock()) {
                throw new RuntimeException("Not enough stock");
            }

            cartItem.setQuantity(newQuantity);

        } else {

            cartItem = new CartItem(
                    cart,
                    product,
                    quantity
            );

            cart.getItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);

        return cart;
    }

    public void removeCartItem(Long cartItemId) {

        cartItemRepository.deleteById(cartItemId);
    }

    public Cart updateQuantity(
            Long cartItemId,
            Integer quantity
    ) {

        CartItem item =
                cartItemRepository.findById(cartItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart item not found"
                                ));

        if (quantity <= 0) {
            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }

        if (quantity > item.getProduct().getStock()) {
            throw new RuntimeException(
                    "Not enough stock"
            );
        }

        item.setQuantity(quantity);

        cartItemRepository.save(item);

        return item.getCart();
    }


    @Transactional
    public void clearCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        cart.getItems().clear();

        cartRepository.save(cart);
    }

}