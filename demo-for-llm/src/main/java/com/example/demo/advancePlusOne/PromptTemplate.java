package com.example.demo.advancePlusOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    private String id;
    private String name;
    private String content;
    private int tokenCount;
}
