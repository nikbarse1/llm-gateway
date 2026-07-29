package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenCounterServiceTest {

    private TokenCounterService tokenCounterService;

    @BeforeEach
    void setUp() {
        tokenCounterService = new TokenCounterService();
    }

    @Test
    void testCountTokens_withValidText() {
        String text = "Hello, how are you today?";
        int tokenCount = tokenCounterService.countTokens(text);
        
        assertTrue(tokenCount > 0, "Token count should be greater than 0");
        assertTrue(tokenCount < text.length(), "Token count should be less than character count");
    }

    @Test
    void testCountTokens_withEmptyString() {
        String text = "";
        int tokenCount = tokenCounterService.countTokens(text);
        
        assertEquals(0, tokenCount, "Empty string should have 0 tokens");
    }

    @Test
    void testCountTokens_withNull() {
        int tokenCount = tokenCounterService.countTokens(null);
        
        assertEquals(0, tokenCount, "Null string should have 0 tokens");
    }

    @Test
    void testCountTokens_withLongText() {
        String text = "This is a longer piece of text that contains multiple sentences. " +
                     "It should demonstrate that the token counter works correctly with " +
                     "longer inputs and provides accurate token counts.";
        int tokenCount = tokenCounterService.countTokens(text);
        
        assertTrue(tokenCount > 10, "Long text should have more than 10 tokens");
    }
}
