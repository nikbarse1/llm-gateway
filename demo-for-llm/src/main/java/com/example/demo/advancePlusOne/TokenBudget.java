package com.example.demo.advancePlusOne;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class TokenBudget {

    int totalBudget;
    int systemReserve;
    int historyReserve;
    int ragReserve;
    int userReserve;
    int responseReserve;

    @Builder.Default
    List<BudgetAction> actions = new ArrayList<>();

    public void addAction(BudgetAction action) {
        this.actions.add(action);
    }

    public record BudgetAction(
            String section,
            int requestedTokens,
            int allocatedTokens,
            int budgetLimit,
            String actionTaken
    ) {}
}
