package com.razorai.backend.service;

import com.razorai.backend.entity.Product;

import java.util.List;

public class AIResponse {

    private String message;
    private String action;
    private List<Product> products;
    private Product recommendedProduct;

    private boolean showChooseButton;
    private boolean showMoreInfoButton;
    private boolean showResearchAgainButton;
    private boolean showCompareButton;
    private boolean showSelectButton;

    private boolean showAddToCartButton;
    private boolean showCheckoutButton;

    public AIResponse() {
    }

    public AIResponse(
            String message,
            String action,
            List<Product> products,
            Product recommendedProduct,
            boolean showChooseButton,
            boolean showMoreInfoButton,
            boolean showResearchAgainButton,
            boolean showCompareButton,
            boolean showSelectButton
    ) {
        this.message = message;
        this.action = action;
        this.products = products;
        this.recommendedProduct = recommendedProduct;
        this.showChooseButton = showChooseButton;
        this.showMoreInfoButton = showMoreInfoButton;
        this.showResearchAgainButton = showResearchAgainButton;
        this.showCompareButton = showCompareButton;
        this.showSelectButton = showSelectButton;
        this.showAddToCartButton = false;
        this.showCheckoutButton = false;
    }

    public AIResponse(
            String message,
            String action,
            List<Product> products,
            Product recommendedProduct,
            boolean showChooseButton,
            boolean showMoreInfoButton,
            boolean showResearchAgainButton,
            boolean showCompareButton,
            boolean showSelectButton,
            boolean showAddToCartButton,
            boolean showCheckoutButton
    ) {
        this.message = message;
        this.action = action;
        this.products = products;
        this.recommendedProduct = recommendedProduct;
        this.showChooseButton = showChooseButton;
        this.showMoreInfoButton = showMoreInfoButton;
        this.showResearchAgainButton = showResearchAgainButton;
        this.showCompareButton = showCompareButton;
        this.showSelectButton = showSelectButton;
        this.showAddToCartButton = showAddToCartButton;
        this.showCheckoutButton = showCheckoutButton;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public Product getRecommendedProduct() {
        return recommendedProduct;
    }

    public void setRecommendedProduct(Product recommendedProduct) {
        this.recommendedProduct = recommendedProduct;
    }

    public boolean isShowChooseButton() {
        return showChooseButton;
    }

    public void setShowChooseButton(boolean showChooseButton) {
        this.showChooseButton = showChooseButton;
    }

    public boolean isShowMoreInfoButton() {
        return showMoreInfoButton;
    }

    public void setShowMoreInfoButton(boolean showMoreInfoButton) {
        this.showMoreInfoButton = showMoreInfoButton;
    }

    public boolean isShowResearchAgainButton() {
        return showResearchAgainButton;
    }

    public void setShowResearchAgainButton(boolean showResearchAgainButton) {
        this.showResearchAgainButton = showResearchAgainButton;
    }

    public boolean isShowCompareButton() {
        return showCompareButton;
    }

    public void setShowCompareButton(boolean showCompareButton) {
        this.showCompareButton = showCompareButton;
    }

    public boolean isShowSelectButton() {
        return showSelectButton;
    }

    public void setShowSelectButton(boolean showSelectButton) {
        this.showSelectButton = showSelectButton;
    }

    public boolean isShowAddToCartButton() {
        return showAddToCartButton;
    }

    public void setShowAddToCartButton(boolean showAddToCartButton) {
        this.showAddToCartButton = showAddToCartButton;
    }

    public boolean isShowCheckoutButton() {
        return showCheckoutButton;
    }

    public void setShowCheckoutButton(boolean showCheckoutButton) {
        this.showCheckoutButton = showCheckoutButton;
    }
}