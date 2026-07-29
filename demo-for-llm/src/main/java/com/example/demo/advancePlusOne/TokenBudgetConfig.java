package com.example.demo.advancePlusOne;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "token.budget")
public class TokenBudgetConfig {

    private int total = 120000;

    private int systemReserve = 10000;

    private int historyReserve = 25000;

    private int ragReserve = 50000;

    private int userReserve = 5000;

    private int responseReserve = 30000;

    private int defaultRagTopK = 5;

    private int minRagTopK = 1;

    private int ragTopKSimple = 2;

    private int ragTopKMedium = 5;

    private int ragTopKComplex = 10;

    private int ragTopKVeryComplex = 20;

    private boolean enabled = true;
}
