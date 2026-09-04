package com.razorai.backend.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.razorai.backend.entity.Product;
import com.razorai.backend.entity.Review;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIService {

    private final CartService cartService;
    private final Client client;
    private final AIToolService aiToolService;
    private final ReviewService reviewService;


    // =====================================================
    // GEMINI MODELS
    // =====================================================

    private static final String PRIMARY_MODEL =
            "gemini-3.7-flash";

    private static final String FALLBACK_MODEL =
            "gemini-3.6-flash";

    private static final int MAX_RETRIES = 3;

    private static final int PRODUCTS_PER_PAGE = 5;


    // =====================================================
    // CONVERSATION MEMORY
    // =====================================================

    private final Map<Long, ConversationState>
            conversations =
            new ConcurrentHashMap<>();


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AIService(
            @Value("${gemini.api.key}") String apiKey,
            AIToolService aiToolService,
            ReviewService reviewService,
            CartService cartService
    ) {

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();

        this.aiToolService =
                aiToolService;

        this.reviewService =
                reviewService;

        this.cartService =
                cartService;
    }


    // =====================================================
    // MAIN CHAT
    // =====================================================

    public AIResponse chat(
            Long userId,
            String userMessage,
            String action,
            List<Long> productIds
    ) {

        ConversationState conversation =
                conversations.computeIfAbsent(
                        userId,
                        id -> new ConversationState()
                );


        // =================================================
        // BUTTON ACTIONS
        // =================================================

        if ("ADD_TO_CART".equalsIgnoreCase(action)) {

            return addProductToCart(
                    userId,
                    productIds,
                    conversation
            );
        }


        if ("CHECKOUT".equalsIgnoreCase(action)) {

            return checkoutResponse();
        }


        if ("CHOOSE_ALL".equalsIgnoreCase(action)) {

            return chooseBestProduct(
                    conversation,
                    conversation.allProducts
            );
        }


        if ("CHOOSE_VISIBLE".equalsIgnoreCase(action)) {

            return chooseBestProduct(
                    conversation,
                    conversation.visibleProducts
            );
        }


        if ("COMPARE_SELECTED".equalsIgnoreCase(action)) {

            return compareSelectedProducts(
                    conversation
            );
        }


        if ("CHOOSE_SELECTED".equalsIgnoreCase(action)) {

            return chooseBestProduct(
                    conversation,
                    conversation.selectedProducts
            );
        }


        if ("MORE_INFO".equalsIgnoreCase(action)) {

            return moreInformation(
                    conversation
            );
        }


        if ("RESEARCH_AGAIN".equalsIgnoreCase(action)) {

            return researchAgain(
                    conversation
            );
        }


        if ("SHOW_MORE".equalsIgnoreCase(action)) {

            return showMoreProducts(
                    conversation
            );
        }


        if ("SELECT_PRODUCTS".equalsIgnoreCase(action)) {

            return selectProducts(
                    conversation,
                    productIds
            );
        }




        // =================================================
        // NORMALIZE MESSAGE
        // =================================================

        if (userMessage == null) {
            userMessage = "";
        }

        userMessage =
                userMessage.trim();


        // =================================================
        // TYPED ADD TO CART
        // =================================================

        if (isAddToCartRequest(userMessage)) {

            if (conversation.recommendedProduct != null) {

                return addProductToCart(
                        userId,
                        List.of(
                                conversation
                                        .recommendedProduct
                                        .getId()
                        ),
                        conversation
                );
            }

            return simpleResponse(
                    "Please select or choose a product first, then I can add it to your cart.",
                    "ASK_PRODUCT"
            );
        }


        // =================================================
        // TYPED CHECKOUT
        // =================================================

        if (isCheckoutRequest(userMessage)) {

            return checkoutResponse();
        }


        // =================================================
        // TYPED COMMANDS
        // =================================================

        if (isChooseSelectedRequest(userMessage)) {

            return chooseBestProduct(
                    conversation,
                    conversation.selectedProducts
            );
        }


        if (isCompareRequest(userMessage)) {

            return compareSelectedProducts(
                    conversation
            );
        }


        if (isChooseVisibleRequest(userMessage)) {

            return chooseBestProduct(
                    conversation,
                    conversation.visibleProducts
            );
        }


        if (isChooseAllRequest(userMessage)) {

            return chooseBestProduct(
                    conversation,
                    conversation.allProducts
            );
        }


        if (isResearchAgainRequest(userMessage)) {

            return researchAgain(
                    conversation
            );
        }


        if (isMoreInfoRequest(userMessage)) {

            return moreInformation(
                    conversation
            );
        }


        if (isShowMoreRequest(userMessage)) {

            return showMoreProducts(
                    conversation
            );
        }


        // =================================================
        // BUDGET
        // =================================================

        if (conversation.waitingForBudget) {

            return handleBudgetAnswer(
                    conversation,
                    userMessage
            );
        }


        // =================================================
        // NORMAL SHOPPING REQUEST
        // =================================================

        return handleShoppingRequest(
                conversation,
                userMessage
        );
    }

    private AIResponse selectProducts(
            ConversationState conversation,
            List<Long> productIds
    ) {

        if (productIds == null || productIds.isEmpty()) {
            return simpleResponse(
                    "Please select at least one product.",
                    "SELECT_PRODUCTS"
            );
        }

        List<Product> selected = new ArrayList<>();

        // Search only from products already shown to the customer
        for (Long productId : productIds) {

            if (productId == null) {
                continue;
            }

            for (Product product : conversation.visibleProducts) {

                if (product != null
                        && product.getId() != null
                        && product.getId().equals(productId)) {

                    if (selected.stream()
                            .noneMatch(p ->
                                    p.getId().equals(productId))) {

                        selected.add(product);
                    }

                    break;
                }
            }

            // Also check all matching products
            if (selected.stream()
                    .noneMatch(p ->
                            p.getId().equals(productId))) {

                for (Product product : conversation.allProducts) {

                    if (product != null
                            && product.getId() != null
                            && product.getId().equals(productId)) {

                        selected.add(product);
                        break;
                    }
                }
            }
        }

        if (selected.isEmpty()) {
            return simpleResponse(
                    "I couldn't find the selected products. Please try again.",
                    "SELECT_PRODUCTS"
            );
        }

        // Save selected products in conversation
        conversation.selectedProducts = selected;

        // First selected product becomes the recommended product
        Product recommended = selected.get(0);
        conversation.recommendedProduct = recommended;

        String message;

        if (selected.size() == 1) {
            message = "Great choice! I've selected " +
                    safeValue(recommended.getName()) +
                    " for you.";
        } else {
            message = "Great! I've selected " +
                    selected.size() +
                    " products for you.";
        }

        return new AIResponse(
                message,
                "PRODUCTS_SELECTED",
                selected,
                recommended,
                true,
                false,
                false,
                false,
                false
        );
    }


    // =====================================================
    // NORMAL SHOPPING REQUEST
    // =====================================================

    private AIResponse handleShoppingRequest(
            ConversationState conversation,
            String userMessage
    ) {

        String extractionPrompt = """
        You are RazorAI's intelligent shopping query understanding system.

        Your job is to understand what the customer wants, even when:
        - They use synonyms
        - They use plural words
        - They use informal product names
        - They use an incorrect product name
        - They describe a product instead of naming it
        - They use natural language
        - They use related product terminology

        Extract:

        1. PRODUCT SEARCH CONCEPTS
        2. MAXIMUM BUDGET

        Return EXACTLY:

        QUERY: <search concepts>
        MAX_PRICE: <number or NONE>

        IMPORTANT RULES FOR QUERY:

        - Understand the customer's actual shopping intent.
        - Do NOT simply copy the customer's exact words.
        - Convert synonyms and related terms into useful search concepts.
        - Include the original important product concept when useful.
        - Include related product terminology when it improves product discovery.
        - Keep the query concise.
        - Do not invent a specific brand or product that the customer did not request.
        - Do not create fake product names.
        - Do not include conversational words.
        - Do not include budget words in QUERY.

        Examples:

        Customer:
        "I want headphones for gaming"

        QUERY:
        gaming headset headphones gaming audio

        MAX_PRICE:
        NONE


        Customer:
        "show me a headset"

        QUERY:
        headset headphones gaming audio

        MAX_PRICE:
        NONE


        Customer:
        "I need something for gaming sound"

        QUERY:
        gaming headset headphones gaming audio

        MAX_PRICE:
        NONE


        Customer:
        "give me earphones for gaming under 3000"

        QUERY:
        gaming headset headphones earphones gaming audio

        MAX_PRICE:
        3000


        Customer:
        "I want a mouse for gaming below 2000"

        QUERY:
        gaming mouse gaming mouse pointer device

        MAX_PRICE:
        2000


        Customer:
        "I need something to type while gaming"

        QUERY:
        gaming keyboard keyboard typing

        MAX_PRICE:
        NONE


        Customer:
        "I want a device to control my computer while gaming"

        QUERY:
        gaming mouse mouse computer input device

        MAX_PRICE:
        NONE


        BUDGET RULES:

        - "under ₹3000" = 3000
        - "below 2500" = 2500
        - "within 2000" = 2000
        - "less than 5000" = 5000
        - "maximum 3000" = 3000
        - "up to 4000" = 4000
        - If there is no maximum budget, use NONE.

        Return ONLY these two fields:

        QUERY: <search concepts>
        MAX_PRICE: <number or NONE>

        Customer message:
        """ + userMessage;


        String extracted =
                generateWithRetry(
                        extractionPrompt
                );


        if (extracted == null ||
                extracted.isBlank()) {

            return simpleResponse(
                    "Sorry, I am unable to connect to the AI service right now. Please try again.",
                    "ERROR"
            );
        }


        String searchQuery =
                extractQuery(extracted);


        BigDecimal maxPrice =
                extractPrice(extracted);


        // =================================================
        // NO BUDGET
        // =================================================

        if (maxPrice == null) {

            conversation.pendingQuery =
                    searchQuery;

            conversation.waitingForBudget =
                    true;


            return simpleResponse(
                    "Sure! Do you have a maximum budget for the "
                            + searchQuery
                            + "? You can give me an amount, or say \"No\" if you have no budget limit.",
                    "ASK_BUDGET"
            );
        }


        conversation.pendingQuery =
                searchQuery;

        conversation.maxBudget =
                maxPrice;

        conversation.hasBudget =
                true;

        conversation.waitingForBudget =
                false;


        return searchProducts(
                conversation,
                searchQuery,
                maxPrice
        );
    }


    // =====================================================
    // HANDLE BUDGET
    // =====================================================

    private AIResponse handleBudgetAnswer(
            ConversationState conversation,
            String userMessage
    ) {

        String lower =
                userMessage.toLowerCase();


        // =================================================
        // NO BUDGET
        // =================================================

        if (lower.equals("no")
                || lower.contains("no budget")
                || lower.contains("no limit")
                || lower.contains("any budget")
                || lower.contains("doesn't matter")
                || lower.contains("dont have a budget")
                || lower.contains("don't have a budget")) {

            conversation.hasBudget =
                    false;

            conversation.maxBudget =
                    null;

            conversation.waitingForBudget =
                    false;


            return searchProducts(
                    conversation,
                    conversation.pendingQuery,
                    null
            );
        }


        // =================================================
        // USER GIVES BUDGET
        // =================================================

        BigDecimal budget =
                extractNumber(userMessage);


        if (budget == null ||
                budget.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return simpleResponse(
                    "Please give me a valid maximum budget, for example ₹3000, or say \"No\" if you have no budget limit.",
                    "ASK_BUDGET"
            );
        }


        conversation.maxBudget =
                budget;

        conversation.hasBudget =
                true;

        conversation.waitingForBudget =
                false;


        return searchProducts(
                conversation,
                conversation.pendingQuery,
                budget
        );
    }


    // =====================================================
    // SEARCH PRODUCTS
    // =====================================================




    // =====================================================
    // SHOW MORE
    // =====================================================

    private AIResponse showMoreProducts(
            ConversationState conversation
    ) {

        if (conversation.allProducts == null ||
                conversation.allProducts.isEmpty()) {

            return simpleResponse(
                    "There are no more products to show.",
                    "NO_PRODUCTS"
            );
        }


        int nextPage =
                conversation.currentPage + 1;


        int start =
                nextPage * PRODUCTS_PER_PAGE;


        if (start >=
                conversation.allProducts.size()) {

            return simpleResponse(
                    "I've shown all the matching products I found.",
                    "NO_MORE_PRODUCTS"
            );
        }


        int end =
                Math.min(
                        start + PRODUCTS_PER_PAGE,
                        conversation.allProducts.size()
                );


        conversation.currentPage =
                nextPage;


        conversation.visibleProducts =
                new ArrayList<>(
                        conversation.allProducts.subList(
                                start,
                                end
                        )
                );


        boolean hasMore =
                end <
                        conversation.allProducts.size();


        String message =
                buildProductListMessage(
                        conversation.visibleProducts,
                        conversation.allProducts.size()
                );


        if (!hasMore) {

            message +=
                    "\n\nThat's all the matching products I found.";
        }


        return new AIResponse(
                message,
                "SHOW_PRODUCTS",
                conversation.visibleProducts,
                null,
                true,
                false,
                false,
                true,
                true
        );
    }


    // =====================================================
    // SELECT PRODUCTS
    // =====================================================

    // =====================================================
// SEARCH PRODUCTS
// =====================================================

    private AIResponse searchProducts(
            ConversationState conversation,
            String query,
            BigDecimal maxPrice
    ) {

        // =================================================
        // 1. NORMAL DATABASE SEARCH
        // =================================================

        List<Product> products =
                aiToolService.searchProducts(
                        query,
                        maxPrice
                );


        // =================================================
        // 2. AI SEMANTIC CATALOG SEARCH
        // =================================================
        //
        // If normal database search cannot understand the
        // customer's wording, Gemini checks the actual
        // product catalog using:
        //
        // - product name
        // - description
        // - category
        // - keywords
        //
        // Example:
        //
        // Customer: "gaming headphones"
        //
        // Product:
        // "Gaming Headset"
        //
        // Gemini can understand that they are related.
        // =================================================

        if (products.isEmpty()) {

            products =
                    searchProductsWithAI(
                            query,
                            maxPrice
                    );
        }


        // =================================================
        // 3. NEAR-BUDGET SEARCH
        // =================================================

        if (products.isEmpty() &&
                maxPrice != null) {

            BigDecimal nearBudget =
                    maxPrice.add(
                            BigDecimal.valueOf(500)
                    );


            // First try normal near-budget search
            products =
                    aiToolService
                            .searchNearBudgetProducts(
                                    query,
                                    nearBudget
                            );


            // If that also fails, try AI semantic
            // matching within the near budget.
            if (products.isEmpty()) {

                products =
                        searchProductsWithAI(
                                query,
                                nearBudget
                        );
            }


            // =================================================
            // NEAR-BUDGET PRODUCTS FOUND
            // =================================================

            if (!products.isEmpty()) {

                // Remove duplicate products
                Map<Long, Product> uniqueProducts =
                        new LinkedHashMap<>();

                for (Product product : products) {

                    if (product.getId() != null) {

                        uniqueProducts.put(
                                product.getId(),
                                product
                        );
                    }
                }

                products =
                        new ArrayList<>(
                                uniqueProducts.values()
                        );


                conversation.allProducts =
                        products;

                conversation.visibleProducts =
                        firstFive(products);

                conversation.currentPage =
                        0;

                conversation.pendingQuery =
                        query;


                return new AIResponse(
                        buildNearBudgetMessage(
                                conversation.visibleProducts,
                                maxPrice
                        ),
                        "SHOW_PRODUCTS",
                        conversation.visibleProducts,
                        null,
                        conversation.visibleProducts.size() > 1,
                        false,
                        false,
                        false,
                        true
                );
            }
        }


        // =================================================
        // 4. NO PRODUCTS
        // =================================================

        if (products.isEmpty()) {

            conversation.allProducts =
                    List.of();

            conversation.visibleProducts =
                    List.of();

            conversation.selectedProducts =
                    new ArrayList<>();


            return simpleResponse(
                    "I couldn't find a suitable product for your request.",
                    "NO_PRODUCTS"
            );
        }


        // =================================================
        // 5. REMOVE DUPLICATES
        // =================================================

        Map<Long, Product> uniqueProducts =
                new LinkedHashMap<>();

        for (Product product : products) {

            if (product.getId() != null) {

                uniqueProducts.put(
                        product.getId(),
                        product
                );
            }
        }

        products =
                new ArrayList<>(
                        uniqueProducts.values()
                );


        // =================================================
        // 6. SAVE FULL RESULTS
        // =================================================

        conversation.allProducts =
                products;

        conversation.currentPage =
                0;

        conversation.pendingQuery =
                query;

        conversation.selectedProducts =
                new ArrayList<>();

        conversation.recommendedProduct =
                null;

        conversation.recommendedProductIds =
                new ArrayList<>();


        // =================================================
        // 7. FIRST FIVE PRODUCTS
        // =================================================

        conversation.visibleProducts =
                firstFive(products);


        // =================================================
        // 8. ONE PRODUCT
        // =================================================

        if (products.size() == 1) {

            Product product =
                    products.get(0);


            conversation.recommendedProduct =
                    product;


            conversation.recommendedProductIds.add(
                    product.getId()
            );


            return new AIResponse(
                    buildSingleProductMessage(
                            product
                    ),
                    "PRODUCT_RECOMMENDATION",
                    conversation.visibleProducts,
                    product,
                    false,
                    true,
                    false,
                    false,
                    false
            );
        }


        // =================================================
        // 9. MULTIPLE PRODUCTS
        // =================================================

        return new AIResponse(
                buildProductListMessage(
                        conversation.visibleProducts,
                        products.size()
                ),
                "SHOW_PRODUCTS",
                conversation.visibleProducts,
                null,
                true,
                false,
                false,
                true,
                true
        );
    }

    // =====================================================
// AI SEMANTIC PRODUCT SEARCH
// =====================================================

    private List<Product> searchProductsWithAI(
            String customerQuery,
            BigDecimal maxPrice
    ) {

        try {

            // =================================================
            // GET REAL PRODUCT CATALOG
            // =================================================

            List<Product> catalog =
                    aiToolService.getAllProductsForAI();


            if (catalog == null ||
                    catalog.isEmpty()) {

                return List.of();
            }


            // =================================================
            // APPLY BUDGET BEFORE GEMINI
            // =================================================

            if (maxPrice != null) {

                catalog =
                        catalog.stream()
                                .filter(product ->
                                        product.getPrice() != null &&
                                                product.getPrice()
                                                        .compareTo(maxPrice) <= 0
                                )
                                .toList();
            }


            if (catalog.isEmpty()) {

                return List.of();
            }


            // =================================================
            // BUILD CATALOG INFORMATION
            // =================================================

            String catalogContext =
                    buildCatalogContext(catalog);


            // =================================================
            // GEMINI PROMPT
            // =================================================

            String prompt = """
                You are RazorAI's product matching AI.

                Your job is to match the customer's shopping
                request against the REAL product catalog.

                CUSTOMER REQUEST:
                %s


                REAL PRODUCT CATALOG:
                %s


                Understand the customer's meaning even when they:

                - use synonyms
                - use singular or plural words
                - use informal product names
                - make spelling mistakes
                - use an incorrect product name
                - describe a product instead of naming it
                - use category names
                - use related product terminology
                - use everyday shopping language


                Examples:

                "headsets"
                "headphones"
                "gaming audio"
                "earphones for gaming"

                These can match a gaming headset if the
                product information supports that match.


                IMPORTANT RULES:

                1. Only select products that actually exist
                   in the provided catalog.

                2. Never invent a product.

                3. Never invent a product ID.

                4. Never create a product that is not in the catalog.

                5. Use the product name, description, category
                   and keywords to determine relevance.

                6. Do not select unrelated products.

                7. Return products in order of relevance.

                8. Only return products that genuinely match
                   the customer's request.

                9. The backend has already applied the customer's
                   maximum budget.

                10. Do not return products above the allowed budget.


                RETURN ONLY THIS FORMAT:

                PRODUCT_IDS: 1,2,3


                If nothing matches:

                PRODUCT_IDS: NONE
                """.formatted(
                    customerQuery,
                    catalogContext
            );


            // =================================================
            // ASK GEMINI
            // =================================================

            String response =
                    generateWithRetry(prompt);


            if (response == null ||
                    response.isBlank()) {

                return List.of();
            }


            // =================================================
            // EXTRACT PRODUCT IDS
            // =================================================

            List<Long> productIds =
                    extractAIProductIds(response);


            if (productIds.isEmpty()) {

                return List.of();
            }


            // =================================================
            // CREATE REAL PRODUCT MAP
            // =================================================

            Map<Long, Product> catalogMap =
                    catalog.stream()
                            .filter(product ->
                                    product.getId() != null)
                            .collect(
                                    Collectors.toMap(
                                            Product::getId,
                                            product -> product,
                                            (existing, replacement) ->
                                                    existing,
                                            LinkedHashMap::new
                                    )
                            );


            // =================================================
            // CONVERT GEMINI IDS INTO REAL PRODUCTS
            // =================================================

            List<Product> matchedProducts =
                    new ArrayList<>();


            for (Long productId :
                    productIds) {

                Product product =
                        catalogMap.get(productId);


                // IMPORTANT:
                // Gemini's ID must exist in our real catalog.
                if (product == null) {
                    continue;
                }


                // Do not recommend products that are out of stock.
                if (product.getStock() == null ||
                        product.getStock() <= 0) {

                    continue;
                }


                // Remove duplicates.
                boolean alreadyAdded =
                        matchedProducts.stream()
                                .anyMatch(existing ->
                                        existing.getId()
                                                .equals(product.getId())
                                );


                if (!alreadyAdded) {

                    matchedProducts.add(
                            product
                    );
                }
            }


            return matchedProducts;

        } catch (Exception e) {

            System.err.println(
                    "AI semantic product search failed: "
                            + e.getMessage()
            );

            // AI search failure should NOT break
            // the customer's shopping experience.
            return List.of();
        }
    }

    // =====================================================
// BUILD AI CATALOG CONTEXT
// =====================================================

    private String buildCatalogContext(
            List<Product> catalog
    ) {

        StringBuilder context =
                new StringBuilder();


        for (Product product :
                catalog) {

            context.append(
                            "PRODUCT_ID: "
                    )
                    .append(
                            product.getId()
                    )
                    .append("\n");


            context.append(
                            "NAME: "
                    )
                    .append(
                            safeValue(
                                    product.getName()
                            )
                    )
                    .append("\n");


            context.append(
                            "DESCRIPTION: "
                    )
                    .append(
                            safeValue(
                                    product.getDescription()
                            )
                    )
                    .append("\n");


            context.append(
                    "CATEGORY: "
            );


            if (product.getCategory() != null) {

                context.append(
                        safeValue(
                                product.getCategory()
                                        .getName()
                        )
                );

            } else {

                context.append(
                        "N/A"
                );
            }


            context.append("\n");


            context.append(
                            "KEYWORDS: "
                    )
                    .append(
                            safeValue(
                                    product.getKeywords()
                            )
                    )
                    .append("\n");


            context.append(
                            "PRICE: ₹"
                    )
                    .append(
                            product.getPrice()
                    )
                    .append("\n");


            context.append(
                            "STOCK: "
                    )
                    .append(
                            product.getStock()
                    )
                    .append("\n");


            context.append(
                    "-------------------------\n"
            );
        }


        return context.toString();
    }

    // =====================================================
// EXTRACT MULTIPLE PRODUCT IDS FROM GEMINI
// =====================================================

    private List<Long> extractAIProductIds(
            String response
    ) {

        List<Long> productIds =
                new ArrayList<>();


        if (response == null) {

            return productIds;
        }


        String text =
                response.trim();


        // =================================================
        // NO MATCH
        // =================================================

        if (text.equalsIgnoreCase(
                "PRODUCT_IDS: NONE"
        )) {

            return productIds;
        }


        // =================================================
        // FIND :
        // =================================================

        int colonIndex =
                text.indexOf(":");


        if (colonIndex == -1) {

            return productIds;
        }


        String idsPart =
                text.substring(
                        colonIndex + 1
                ).trim();


        if (idsPart.equalsIgnoreCase(
                "NONE"
        )) {

            return productIds;
        }


        // =================================================
        // SPLIT IDS
        // =================================================

        String[] parts =
                idsPart.split(",");


        for (String part :
                parts) {

            try {

                String cleaned =
                        part.trim()
                                .replaceAll(
                                        "[^0-9]",
                                        ""
                                );


                if (!cleaned.isEmpty()) {

                    Long id =
                            Long.parseLong(
                                    cleaned
                            );


                    if (!productIds.contains(id)) {

                        productIds.add(id);
                    }
                }

            } catch (NumberFormatException ignored) {

                // Ignore invalid IDs returned by Gemini.
            }
        }


        return productIds;
    }

    // =====================================================
// SAFE STRING VALUE
// =====================================================

    private String safeValue(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return "N/A";
        }

        return value;
    }

    // =====================================================
    // COMPARE / RATE SELECTED PRODUCTS
    // =====================================================

    private AIResponse compareSelectedProducts(
            ConversationState conversation
    ) {

        List<Product> selected =
                conversation.selectedProducts;


        if (selected == null ||
                selected.size() < 2) {

            return simpleResponse(
                    "Please select at least two products if you want me to compare them.",
                    "SELECT_PRODUCTS"
            );
        }


        StringBuilder context =
                new StringBuilder();


        for (Product product :
                selected) {

            appendProductWithReviews(
                    context,
                    product
            );
        }


        String prompt = """
                You are RazorAI's product comparison engine.

                The customer selected these products.

                Compare ONLY these products.

                Give every product an overall score from 1 to 5.

                The score is an AI comparison score, NOT a customer rating.

                Consider ONLY information provided in the product data:

                - Customer requirements
                - Price
                - Budget
                - Description
                - Features / keywords
                - Stock
                - Real customer reviews
                - Positive review points
                - Negative review points
                - Overall value

                IMPORTANT:

                - Never invent specifications.
                - Never invent customer reviews.
                - Never invent ratings.
                - Never invent prices.
                - Do not compare products that are not provided.
                - Give each product a score between 1 and 5.
                - Explain briefly why it received that score.

                Return a concise comparison.

                At the end ask:

                "Would you like me to choose one product from these selected products?"

                SELECTED PRODUCTS:

                %s
                """.formatted(
                context
        );


        String answer =
                generateWithRetry(prompt);


        if (answer == null ||
                answer.isBlank()) {

            return simpleResponse(
                    "I couldn't compare the selected products right now. Please try again.",
                    "ERROR"
            );
        }


        return new AIResponse(
                answer,
                "COMPARE_SELECTED",
                selected,
                null,
                false,
                false,
                true,
                false,
                false
        );
    }


    // =====================================================
    // CHOOSE BEST PRODUCT
    // =====================================================

    private AIResponse chooseBestProduct(
            ConversationState conversation,
            List<Product> candidates
    ) {

        if (candidates == null ||
                candidates.isEmpty()) {

            return simpleResponse(
                    "I don't have enough products to choose from yet.",
                    "NO_PRODUCTS"
            );
        }


        // =================================================
        // EXCLUDE PREVIOUSLY RECOMMENDED
        // =================================================

        List<Product> available =
                new ArrayList<>();


        for (Product product :
                candidates) {

            if (!conversation.recommendedProductIds
                    .contains(product.getId())) {

                available.add(product);
            }
        }


        if (available.isEmpty()) {

            return simpleResponse(
                    "I've already recommended all the suitable products in this group.",
                    "NO_MORE_PRODUCTS"
            );
        }


        StringBuilder context =
                new StringBuilder();


        for (Product product :
                available) {

            appendProductWithReviews(
                    context,
                    product
            );
        }


        String prompt = """
                You are RazorAI's product decision engine.

                Choose EXACTLY ONE product from the provided products.

                Consider:

                - Customer requirements
                - Customer budget
                - Price
                - Value
                - Product description
                - Features
                - Stock
                - REAL customer reviews
                - Positive review points
                - Negative review points

                IMPORTANT:

                - Choose ONLY from the provided products.
                - Never invent products.
                - Never invent specifications.
                - Never invent prices.
                - Never invent reviews.
                - Never invent ratings.

                Return exactly:

                PRODUCT_ID: <id>
                REASON: <short explanation>

                PRODUCTS:

                %s
                """.formatted(
                context
        );


        String decision =
                generateWithRetry(prompt);


        if (decision == null ||
                decision.isBlank()) {

            return simpleResponse(
                    "I couldn't choose a product right now. Please try again.",
                    "ERROR"
            );
        }


        Long productId =
                extractProductId(
                        decision
                );


        Product selected =
                findProductById(
                        available,
                        productId
                );


        if (selected == null) {

            selected =
                    available.get(0);
        }


        conversation.recommendedProduct =
                selected;


        conversation.recommendedProductIds.add(
                selected.getId()
        );


        String reason =
                extractReason(
                        decision
                );


        return new AIResponse(
                buildRecommendationMessage(
                        selected,
                        reason
                ),
                "PRODUCT_RECOMMENDATION",
                List.of(selected),
                selected,
                false,
                true,
                available.size() > 1,
                false,
                false
        );
    }


    // =====================================================
    // RESEARCH AGAIN
    // =====================================================

    private AIResponse researchAgain(
            ConversationState conversation
    ) {

        if (conversation.selectedProducts != null &&
                conversation.selectedProducts.size() > 1) {

            return chooseBestProduct(
                    conversation,
                    conversation.selectedProducts
            );
        }


        return chooseBestProduct(
                conversation,
                conversation.allProducts
        );
    }


    // =====================================================
    // MORE INFORMATION
    // =====================================================

    private AIResponse moreInformation(
            ConversationState conversation
    ) {

        Product product =
                conversation.recommendedProduct;


        if (product == null &&
                conversation.selectedProducts != null &&
                conversation.selectedProducts.size() == 1) {

            product =
                    conversation.selectedProducts.get(0);
        }


        if (product == null &&
                conversation.visibleProducts != null &&
                conversation.visibleProducts.size() == 1) {

            product =
                    conversation.visibleProducts.get(0);
        }


        if (product == null) {

            return simpleResponse(
                    "Which product would you like to know more about?",
                    "ASK_PRODUCT"
            );
        }


        StringBuilder context =
                new StringBuilder();


        appendProductWithReviews(
                context,
                product
        );


        String prompt = """
                You are RazorAI.

                Give the customer more information about this product.

                Use ONLY the provided information.

                Explain:

                - Product description
                - Price
                - Stock
                - Category
                - Features / keywords
                - What customers like
                - What customers dislike
                - Customer review sentiment

                IMPORTANT:

                - Never invent specifications.
                - Never invent reviews.
                - Never invent ratings.
                - Never invent prices.

                PRODUCT:

                %s

                Give a concise but useful answer.
                """.formatted(
                context
        );


        String answer =
                generateWithRetry(prompt);


        if (answer == null ||
                answer.isBlank()) {

            return simpleResponse(
                    "I couldn't get the additional product information right now. Please try again.",
                    "ERROR"
            );
        }


        return new AIResponse(
                answer,
                "MORE_INFORMATION",
                List.of(product),
                product,
                false,
                true,
                true,
                false,
                false
        );
    }


    // =====================================================
    // ADD PRODUCT TO CART
    // =====================================================

    private AIResponse addProductToCart(
            Long userId,
            List<Long> productIds,
            ConversationState conversation
    ) {

        if (productIds == null ||
                productIds.isEmpty()) {

            return simpleResponse(
                    "Please select a product to add to your cart.",
                    "ADD_TO_CART"
            );
        }


        Product addedProduct = null;


        // =================================================
        // FIND PRODUCT
        // =================================================

        for (Long productId :
                productIds) {

            Product product =
                    findProductById(
                            conversation.allProducts,
                            productId
                    );


            /*
             * If the product is not in the current
             * conversation,
             * check the recommended product.
             */
            if (product == null &&
                    conversation.recommendedProduct != null &&
                    conversation.recommendedProduct
                            .getId()
                            .equals(productId)) {

                product =
                        conversation.recommendedProduct;
            }

            /*
             * IMPORTANT:
             * Complementary products are also valid
             * products that the customer can add.
             *
             * Example:
             * Headset added
             * -> AI recommends Gaming Mouse
             * -> customer clicks "Add to Cart"
             * -> Gaming Mouse must be found here.
             */
            if (product == null &&
                    conversation.complementaryProducts != null) {

                product =
                        findProductById(
                                conversation.complementaryProducts,
                                productId
                        );
            }


            if (product == null) {
                continue;
            }


            try {

                // =================================================
                // IMPORTANT:
                // Your CartService method is addProductToCart()
                // =================================================

                cartService.addProductToCart(
                        userId,
                        productId,
                        1
                );


                addedProduct =
                        product;

                break;

            } catch (RuntimeException e) {

                return simpleResponse(
                        "I couldn't add "
                                + product.getName()
                                + " to your cart: "
                                + e.getMessage(),
                        "CART_ERROR"
                );
            }
        }


        // =================================================
        // PRODUCT NOT FOUND
        // =================================================

        if (addedProduct == null) {

            return simpleResponse(
                    "I couldn't find that product. Please select a product from the recommendations.",
                    "ERROR"
            );
        }


        // =================================================
        // FIND COMPLEMENTARY PRODUCTS
        // =================================================

        List<Product> complementary =
                aiToolService
                        .searchComplementaryProducts(
                                addedProduct
                        );


        /*
         * Remove the product itself.
         */

        Product finalAddedProduct = addedProduct;

        complementary =
                complementary.stream()
                        .filter(product ->
                                product.getId() != null
                                        && !product.getId().equals(
                                        finalAddedProduct.getId()
                                )
                        )
                        .limit(3)
                        .toList();


        // =================================================
        // SAVE COMPLEMENTARY PRODUCTS
        // =================================================

        conversation.complementaryProducts =
                new ArrayList<>(
                        complementary
                );


        // =================================================
        // BUILD MESSAGE
        // =================================================

        StringBuilder message =
                new StringBuilder();


        message.append(
                        "🛒 "
                )
                .append(
                        addedProduct.getName()
                )
                .append(
                        " has been added to your cart."
                );


        if (!complementary.isEmpty()) {

            message.append(
                            "\n\nSince you chose "
                    )
                    .append(
                            addedProduct.getName()
                    )
                    .append(
                            ", you may also like:"
                    );


            for (Product product :
                    complementary) {

                message.append(
                                "\n• "
                        )
                        .append(
                                product.getName()
                        )
                        .append(
                                " — ₹"
                        )
                        .append(
                                product.getPrice()
                        );
            }


            message.append(
                    "\n\nWould you like to add anything else, or proceed to checkout?"
            );

        } else {

            message.append(
                    "\n\nYour cart is ready. Would you like to checkout?"
            );
        }


        return new AIResponse(
                message.toString(),
                "CART_UPDATED",
                complementary,
                addedProduct,
                false,
                false,
                false,
                false,
                false
        );
    }


    // =====================================================
    // CHECKOUT
    // =====================================================

    private AIResponse checkoutResponse() {

        return new AIResponse(
                "Your cart is ready. Please proceed to checkout to review your order and continue to payment.",
                "CHECKOUT",
                List.of(),
                null,
                false,
                false,
                false,
                false,
                false
        );
    }


    // =====================================================
    // PRODUCT + REVIEWS
    // =====================================================

    private void appendProductWithReviews(
            StringBuilder context,
            Product product
    ) {

        context.append(
                        "Product ID: "
                )
                .append(product.getId())
                .append("\n");


        context.append(
                        "Name: "
                )
                .append(product.getName())
                .append("\n");


        context.append(
                        "Description: "
                )
                .append(product.getDescription())
                .append("\n");


        context.append(
                        "Price: ₹"
                )
                .append(product.getPrice())
                .append("\n");


        context.append(
                        "Stock: "
                )
                .append(product.getStock())
                .append("\n");


        context.append(
                        "Keywords: "
                )
                .append(product.getKeywords())
                .append("\n");


        if (product.getCategory() != null) {

            context.append(
                            "Category: "
                    )
                    .append(
                            product.getCategory().getName()
                    )
                    .append("\n");
        }


        List<Review> reviews =
                reviewService.getProductReviews(
                        product.getId()
                );


        if (reviews.isEmpty()) {

            context.append(
                    "Customer Reviews: No reviews available.\n"
            );

        } else {

            context.append(
                    "REAL CUSTOMER REVIEWS:\n"
            );


            for (Review review :
                    reviews) {

                context.append(
                                "- Rating: "
                        )
                        .append(
                                review.getRating()
                        )
                        .append(
                                "/5, Comment: "
                        )
                        .append(
                                review.getComment()
                        )
                        .append("\n");
            }
        }


        context.append("\n");
    }


    // =====================================================
    // PRODUCT LIST MESSAGE
    // =====================================================

    private String buildProductListMessage(
            List<Product> products,
            int totalProducts
    ) {

        StringBuilder message =
                new StringBuilder();


        message.append(
                        "I found "
                )
                .append(totalProducts)
                .append(
                        " matching products. Here are the first "
                )
                .append(products.size())
                .append(":\n\n");


        for (Product product :
                products) {

            message.append("• ")
                    .append(product.getName())
                    .append(" — ₹")
                    .append(product.getPrice())
                    .append("\n");
        }


        message.append(
                "\nYou can select the products you like and I can compare them for you."
        );


        message.append(
                "\n\n✨ I can also choose the best one from all the matching products."
        );


        return message.toString();
    }


    // =====================================================
    // SELECTED PRODUCTS MESSAGE
    // =====================================================

    private String buildSelectedProductsMessage(
            List<Product> products
    ) {

        StringBuilder message =
                new StringBuilder();


        message.append(
                        "You selected "
                )
                .append(products.size())
                .append(
                        " products:\n\n"
                );


        for (Product product :
                products) {

            message.append("• ")
                    .append(product.getName())
                    .append(" — ₹")
                    .append(product.getPrice())
                    .append("\n");
        }


        message.append(
                "\n⭐ Would you like me to compare and rate these products from 1 to 5?"
        );


        return message.toString();
    }


    // =====================================================
    // NEAR BUDGET MESSAGE
    // =====================================================

    private String buildNearBudgetMessage(
            List<Product> products,
            BigDecimal budget
    ) {

        StringBuilder message =
                new StringBuilder();


        message.append(
                        "I couldn't find a product within your "
                )
                .append("₹")
                .append(budget)
                .append(
                        " budget.\n\n"
                );


        message.append(
                "Here are options up to ₹500 above your budget:\n\n"
        );


        for (Product product :
                products) {

            BigDecimal difference =
                    product.getPrice()
                            .subtract(budget);


            message.append("• ")
                    .append(product.getName())
                    .append(" — ₹")
                    .append(product.getPrice())
                    .append(
                            " (₹"
                    )
                    .append(difference)
                    .append(
                            " above budget)\n"
                    );
        }


        return message.toString();
    }


    // =====================================================
    // SINGLE PRODUCT MESSAGE
    // =====================================================

    private String buildSingleProductMessage(
            Product product
    ) {

        return "I found one suitable option:\n\n"
                + "⭐ "
                + product.getName()
                + "\n"
                + "₹"
                + product.getPrice()
                + "\n\n"
                + "You can ask me for more information about this product or add it to your cart.";
    }


    // =====================================================
    // RECOMMENDATION MESSAGE
    // =====================================================

    private String buildRecommendationMessage(
            Product product,
            String reason
    ) {

        return "⭐ My recommendation\n\n"
                + product.getName()
                + " — ₹"
                + product.getPrice()
                + "\n\n"
                + "Why I chose it:\n"
                + reason
                + "\n\n"
                + "You can ask me for more information, add it to your cart, or research another option.";
    }


    // =====================================================
    // REQUEST DETECTION
    // =====================================================

    private boolean isChooseAllRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "choose the best for me"
        )
                || text.contains(
                "choose the best one for me"
        )
                || text.contains(
                "choose one from all"
        )
                || text.contains(
                "choose from all"
        )
                || text.contains(
                "pick the best from all"
        )
                || text.contains(
                "you choose from all"
        );
    }


    private boolean isChooseVisibleRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "choose from these"
        )
                || text.contains(
                "choose from these five"
        )
                || text.contains(
                "choose from the five"
        )
                || text.contains(
                "pick from these"
        )
                || text.contains(
                "choose one from these"
        );
    }


    private boolean isChooseSelectedRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "choose one from selected"
        )
                || text.contains(
                "choose from selected"
        )
                || text.contains(
                "choose one from my selected"
        )
                || text.contains(
                "pick one from selected"
        )
                || text.equals(
                "choose one"
        )
                || text.equals(
                "pick one"
        );
    }


    private boolean isCompareRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "compare these"
        )
                || text.contains(
                "compare them"
        )
                || text.contains(
                "rate these"
        )
                || text.contains(
                "rate them"
        )
                || text.contains(
                "compare and rate"
        )
                || text.contains(
                "which one is better"
        );
    }


    private boolean isResearchAgainRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "research again"
        )
                || text.contains(
                "choose another"
        )
                || text.contains(
                "pick another"
        )
                || text.contains(
                "another one"
        )
                || text.contains(
                "different one"
        )
                || text.contains(
                "try another"
        )
                || text.contains(
                "i don't like this"
        )
                || text.contains(
                "i dont like this"
        );
    }


    private boolean isMoreInfoRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.contains(
                "tell me more"
        )
                || text.contains(
                "more information"
        )
                || text.contains(
                "more info"
        )
                || text.contains(
                "product details"
        )
                || text.contains(
                "what are the features"
        )
                || text.contains(
                "what do customers think"
        )
                || text.contains(
                "customer reviews"
        )
                || text.contains(
                "about it"
        )
                || text.contains(
                "about this product"
        );
    }


    private boolean isShowMoreRequest(
            String message
    ) {

        String text =
                message.toLowerCase();


        return text.equals("more")
                || text.contains("show more")
                || text.contains("more products")
                || text.contains("show me more")
                || text.contains("next products")
                || text.contains("next five");
    }


    // =====================================================
    // ADD TO CART REQUEST
    // =====================================================

    private boolean isAddToCartRequest(
            String message
    ) {

        String text =
                message.toLowerCase().trim();


        return text.contains(
                "add to cart"
        )
                || text.contains(
                "add this to cart"
        )
                || text.equals(
                "add this"
        )
                || text.equals(
                "add it"
        )
                || text.equals(
                "add"
        );
    }


    // =====================================================
    // CHECKOUT REQUEST
    // =====================================================

    private boolean isCheckoutRequest(
            String message
    ) {

        String text =
                message.toLowerCase().trim();


        return text.equals(
                "checkout"
        )
                || text.contains(
                "go to checkout"
        )
                || text.contains(
                "proceed to checkout"
        )
                || text.contains(
                "take me to checkout"
        )
                || text.contains(
                "i want to checkout"
        )
                || text.contains(
                "no thanks checkout"
        )
                || text.contains(
                "nothing else checkout"
        )
                || text.contains(
                "no thanks"
        )
                || text.contains(
                "nothing else"
        )
                || text.contains(
                "that's all"
        )
                || text.contains(
                "thats all"
        );
    }


    // =====================================================
    // EXTRACTION
    // =====================================================

    private String extractQuery(
            String extracted
    ) {

        for (String line :
                extracted.split("\\R")) {

            line = line.trim();


            if (line.startsWith("QUERY:")) {

                return line.substring(
                        "QUERY:".length()
                ).trim();
            }
        }


        return "";
    }


    private BigDecimal extractPrice(
            String extracted
    ) {

        for (String line :
                extracted.split("\\R")) {

            line = line.trim();


            if (line.startsWith("MAX_PRICE:")) {

                String value =
                        line.substring(
                                "MAX_PRICE:".length()
                        ).trim();


                if (value.equalsIgnoreCase(
                        "NONE"
                )) {

                    return null;
                }


                return extractNumber(value);
            }
        }


        return null;
    }


    private BigDecimal extractNumber(
            String text
    ) {

        try {

            String cleaned =
                    text.replaceAll(
                            "[^0-9.]",
                            ""
                    );


            if (cleaned.isEmpty()) {
                return null;
            }


            return new BigDecimal(
                    cleaned
            );

        } catch (Exception e) {

            return null;
        }
    }


    private Long extractProductId(
            String text
    ) {

        for (String line :
                text.split("\\R")) {

            line = line.trim();


            if (line.startsWith(
                    "PRODUCT_ID:"
            )) {

                try {

                    return Long.parseLong(
                            line.substring(
                                    "PRODUCT_ID:"
                                            .length()
                            ).trim()
                    );

                } catch (Exception ignored) {

                    return null;
                }
            }
        }


        return null;
    }


    private String extractReason(
            String text
    ) {

        for (String line :
                text.split("\\R")) {

            line = line.trim();


            if (line.startsWith(
                    "REASON:"
            )) {

                return line.substring(
                        "REASON:".length()
                ).trim();
            }
        }


        return "It provides the best overall balance based on the available product information and customer feedback.";
    }


    // =====================================================
    // PRODUCT HELPERS
    // =====================================================

    private List<Product> firstFive(
            List<Product> products
    ) {

        int end =
                Math.min(
                        PRODUCTS_PER_PAGE,
                        products.size()
                );


        return new ArrayList<>(
                products.subList(
                        0,
                        end
                )
        );
    }


    private Product findProductById(
            List<Product> products,
            Long id
    ) {

        if (products == null ||
                id == null) {

            return null;
        }


        for (Product product :
                products) {

            if (product.getId()
                    .equals(id)) {

                return product;
            }
        }


        return null;
    }


    private boolean containsProduct(
            List<Product> products,
            Product product
    ) {

        for (Product existing :
                products) {

            if (existing.getId()
                    .equals(product.getId())) {

                return true;
            }
        }


        return false;
    }


    // =====================================================
    // GEMINI
    // =====================================================

    private String generateWithRetry(
            String prompt
    ) {

        String response =
                tryModelWithRetry(
                        PRIMARY_MODEL,
                        prompt
                );


        if (response != null) {
            return response;
        }


        System.out.println(
                "Primary Gemini model failed. Trying fallback model."
        );


        return tryModelWithRetry(
                FALLBACK_MODEL,
                prompt
        );
    }


    private String tryModelWithRetry(
            String model,
            String prompt
    ) {

        for (int attempt = 1;
             attempt <= MAX_RETRIES;
             attempt++) {

            try {

                System.out.println(
                        "Gemini request: "
                                + model
                                + " attempt "
                                + attempt
                );


                GenerateContentResponse response =
                        client.models.generateContent(
                                model,
                                prompt,
                                null
                        );


                if (response != null &&
                        response.text() != null &&
                        !response.text().isBlank()) {

                    return response.text();
                }

            } catch (Exception e) {

                System.err.println(
                        "Gemini error: "
                                + e.getMessage()
                );


                if (!isRetryableError(e)) {

                    return null;
                }


                if (attempt == MAX_RETRIES) {
                    break;
                }


                long delay =
                        (long) Math.pow(
                                2,
                                attempt
                        ) * 1000;


                try {

                    Thread.sleep(delay);

                } catch (InterruptedException interrupted) {

                    Thread.currentThread()
                            .interrupt();

                    return null;
                }
            }
        }


        return null;
    }


    private boolean isRetryableError(
            Exception e
    ) {

        String message =
                e.getMessage();


        if (message == null) {
            return false;
        }


        String error =
                message.toLowerCase();


        return error.contains("503")
                || error.contains(
                "service unavailable"
        )
                || error.contains(
                "high demand"
        )
                || error.contains(
                "temporarily unavailable"
        )
                || error.contains("429")
                || error.contains(
                "rate limit"
        )
                || error.contains(
                "too many requests"
        )
                || error.contains("500")
                || error.contains(
                "internal server error"
        );
    }


    // =====================================================
    // SIMPLE RESPONSE
    // =====================================================

    private AIResponse simpleResponse(
            String message,
            String action
    ) {

        return new AIResponse(
                message,
                action,
                List.of(),
                null,
                false,
                false,
                false,
                false,
                false
        );
    }


    // =====================================================
    // CONVERSATION STATE
    // =====================================================

    private static class ConversationState {

        private String pendingQuery;

        private BigDecimal maxBudget;

        private boolean hasBudget;

        private boolean waitingForBudget;


        // =================================================
        // ALL MATCHING PRODUCTS
        // =================================================

        private List<Product> allProducts =
                new ArrayList<>();


        // =================================================
        // CURRENT VISIBLE PRODUCTS
        // =================================================

        private List<Product> visibleProducts =
                new ArrayList<>();


        // =================================================
        // SELECTED PRODUCTS
        // =================================================

        private List<Product> selectedProducts =
                new ArrayList<>();


        // =================================================
        // CURRENT RECOMMENDED PRODUCT
        // =================================================

        private Product recommendedProduct;


        // =================================================
        // PRODUCTS ALREADY RECOMMENDED
        // =================================================

        private List<Long> recommendedProductIds =
                new ArrayList<>();


        // =================================================
        // COMPLEMENTARY PRODUCTS
        // =================================================

        private List<Product> complementaryProducts =
                new ArrayList<>();


        // =================================================
        // PAGINATION
        // =================================================

        private int currentPage = 0;
    }
}