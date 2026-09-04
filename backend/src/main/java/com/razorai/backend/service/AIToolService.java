package com.razorai.backend.service;

import com.razorai.backend.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIToolService {

    private final ProductService productService;

    public AIToolService(ProductService productService) {
        this.productService = productService;
    }


    // =====================================================
    // SEARCH PRODUCTS
    // =====================================================

    public List<Product> searchProducts(
            String query,
            BigDecimal maxPrice
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return List.of();
        }

        /*
         * First use the normal database search.
         *
         * This keeps the search fast when the customer
         * uses the actual product name or keywords.
         */

        List<Product> products =
                productService.searchProductsWithinBudget(
                        query.trim(),
                        maxPrice
                );

        return removeDuplicates(products);
    }


    // =====================================================
    // SEARCH NEAR BUDGET
    // =====================================================

    public List<Product> searchNearBudgetProducts(
            String query,
            BigDecimal nearBudget
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return List.of();
        }

        List<Product> products =
                productService.searchProductsNearBudget(
                        query.trim(),
                        nearBudget
                );

        return removeDuplicates(products);
    }


    // =====================================================
    // SEARCH ALL PRODUCTS
    // =====================================================

    /*
     * This method gives the AI access to the REAL catalog.
     *
     * Gemini must only choose products returned from here.
     */

    public List<Product> getAllProductsForAI() {

        return removeDuplicates(
                productService.getAllProducts()
        );
    }


    // =====================================================
    // SEARCH COMPLEMENTARY PRODUCTS
    // =====================================================

    public List<Product> searchComplementaryProducts(
            Product mainProduct
    ) {

        if (mainProduct == null) {
            return List.of();
        }


        String category =
                mainProduct.getCategory() != null
                        ? mainProduct.getCategory().getName()
                        : "";


        String keywords =
                mainProduct.getKeywords();


        String query =
                buildComplementaryQuery(
                        mainProduct,
                        category,
                        keywords
                );


        if (query == null ||
                query.trim().isEmpty()) {

            return List.of();
        }


        List<Product> products =
                productService.searchProductsByWords(
                        query
                );


        /*
         * Remove the product that was already added.
         */

        return removeDuplicates(products)
                .stream()
                .filter(product ->
                        product.getId() != null
                                && !product.getId()
                                .equals(mainProduct.getId())
                )
                .limit(10)
                .toList();
    }


    // =====================================================
    // BUILD COMPLEMENTARY QUERY
    // =====================================================

    private String buildComplementaryQuery(
            Product product,
            String category,
            String keywords
    ) {

        if (product.getName() == null) {
            return "";
        }


        String name =
                product.getName()
                        .toLowerCase();


        // =================================================
        // GAMING MOUSE
        // =================================================

        if (containsAny(
                name,
                "mouse",
                "gaming mouse"
        )) {

            return "keyboard";
        }


        // =================================================
        // GAMING KEYBOARD
        // =================================================

        if (containsAny(
                name,
                "keyboard",
                "gaming keyboard"
        )) {

            return "mouse";
        }


        // =================================================
        // HEADSET / HEADPHONES
        // =================================================

        if (containsAny(
                name,
                "headset",
                "headphone",
                "headphones"
        )) {

            return "keyboard mouse";
        }


        // =================================================
        // LAPTOP
        // =================================================

        if (containsAny(
                name,
                "laptop"
        )) {

            return "mouse keyboard";
        }


        // =================================================
        // PHONE / MOBILE
        // =================================================

        if (containsAny(
                name,
                "phone",
                "mobile",
                "smartphone"
        )) {

            return "headphones";
        }


        // =================================================
        // FALLBACK
        // =================================================

        return "";
    }


    // =====================================================
    // CHECK WHETHER TEXT CONTAINS ANY VALUE
    // =====================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        for (String value :
                values) {

            if (text.contains(value)) {
                return true;
            }
        }

        return false;
    }


    // =====================================================
    // REMOVE DUPLICATES
    // =====================================================

    private List<Product> removeDuplicates(
            List<Product> products
    ) {

        if (products == null ||
                products.isEmpty()) {

            return List.of();
        }


        List<Product> uniqueProducts =
                new ArrayList<>();


        for (Product product :
                products) {

            if (product == null ||
                    product.getId() == null) {

                continue;
            }


            boolean exists =
                    uniqueProducts.stream()
                            .anyMatch(existing ->
                                    existing.getId()
                                            .equals(product.getId())
                            );


            if (!exists) {
                uniqueProducts.add(product);
            }
        }


        return uniqueProducts;
    }
}