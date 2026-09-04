package com.razorai.backend.controller;

import com.razorai.backend.service.AIResponse;
import com.razorai.backend.service.AIService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173")
public class AIController {

    private final AIService aiService;


    public AIController(AIService aiService) {
        this.aiService = aiService;
    }


    @PostMapping("/chat")
    public AIResponse chat(
            @RequestParam(defaultValue = "1")
            Long userId,

            @RequestBody
            ChatRequest request
    ) {

        return aiService.chat(
                userId,
                request.getMessage(),
                request.getAction(),
                request.getProductIds()
        );
    }


    public static class ChatRequest {

        private String message;

        private String action;

        private List<Long> productIds;


        public ChatRequest() {
        }


        public String getMessage() {
            return message;
        }


        public void setMessage(
                String message
        ) {

            this.message = message;
        }


        public String getAction() {
            return action;
        }


        public void setAction(
                String action
        ) {

            this.action = action;
        }


        public List<Long> getProductIds() {
            return productIds;
        }


        public void setProductIds(
                List<Long> productIds
        ) {

            this.productIds = productIds;
        }
    }
}