package com.example.demo;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final OpenAiChatModel model;

    public TestController(OpenAiChatModel model) {
        this.model = model;
    }

    @GetMapping("/test")
    public String test() {
        return model.call("Say hello");
    }
}
