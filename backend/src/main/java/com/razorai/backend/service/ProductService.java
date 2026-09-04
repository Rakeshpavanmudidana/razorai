package com.razorai.backend.service;

import com.razorai.backend.entity.Category;
import com.razorai.backend.entity.Product;
import com.razorai.backend.repository.CategoryRepository;
import com.razorai.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );
    }

    public Product createProduct(Product product) {

        Long categoryId = product.getCategory().getId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new RuntimeException("Category not found")
                );

        product.setCategory(category);

        return productRepository.save(product);
    }

    public Product updateProduct(
            Long id,
            Product updatedProduct
    ) {

        Product product = getProductById(id);

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setImageUrl(updatedProduct.getImageUrl());
        product.setKeywords(updatedProduct.getKeywords());

        Long categoryId =
                updatedProduct.getCategory().getId();

        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found"
                                )
                        );

        product.setCategory(category);

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> searchProducts(String name) {

        return productRepository
                .findByNameContainingIgnoreCase(name);
    }

    public List<Product> getProductsByCategory(
            Long categoryId
    ) {

        return productRepository
                .findByCategoryId(categoryId);
    }

    // --------------------------------------------------
    // AI PRODUCT SEARCH
    // --------------------------------------------------

    public List<Product> searchProductsForAI(
            String query
    ) {

        return searchProductsByWords(query);
    }

    public List<Product> searchProductsByWords(String query) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String[] words = query.trim().split("\\s+");

        List<Product> results = new ArrayList<>();

        for (String word : words) {

            if (word.length() < 2) {
                continue;
            }

            List<Product> products =
                    productRepository
                            .findByNameContainingIgnoreCaseOrKeywordsContainingIgnoreCase(
                                    word,
                                    word
                            );

            for (Product product : products) {

                boolean alreadyExists = results.stream()
                        .anyMatch(existing ->
                                existing.getId() != null
                                        && existing.getId().equals(product.getId())
                        );

                if (!alreadyExists) {
                    results.add(product);
                }
            }
        }

        return results;
    }

    // --------------------------------------------------
    // AI BUDGET SEARCH
    // --------------------------------------------------

    public List<Product> searchProductsByWordsAndMaxPrice(
            String query,
            BigDecimal maxPrice
    ) {

        List<Product> products =
                searchProductsByWords(query);

        if (maxPrice == null) {
            return products;
        }

        return products.stream()
                .filter(product ->
                        product.getPrice()
                                .compareTo(maxPrice) <= 0
                )
                .toList();
    }

    public List<Product> searchProductsWithinBudget(
            String query,
            BigDecimal maxPrice
    ) {

        return searchProductsByWordsAndMaxPrice(
                query,
                maxPrice
        );
    }

    public List<Product> searchProductsNearBudget(
            String query,
            BigDecimal nearBudget
    ) {

        return searchProductsByWordsAndMaxPrice(
                query,
                nearBudget
        )
                .stream()
                .sorted((a, b) ->
                        a.getPrice()
                                .compareTo(b.getPrice())
                )
                .toList();
    }
}