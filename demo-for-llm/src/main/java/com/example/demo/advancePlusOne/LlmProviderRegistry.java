package com.example.demo.advancePlusOne;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LlmProviderRegistry {

    private final Map<String, LlmProvider> providerMap;

    // Spring automatically injects all active classes implementing LlmProvider into this list
    public LlmProviderRegistry(List<LlmProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.getProviderName().toUpperCase(),
                        Function.identity()
                ));
        log.info("Gateway AI Registry initialized successfully. Supported engines: {}", providerMap.keySet());
    }

    /**
     * Resolves the desired target AI service strategy block dynamically at runtime.
     */
    public LlmProvider getProvider(String providerName) {
        return Optional.ofNullable(providerMap.get(providerName.toUpperCase()))
                .orElseThrow(() -> {
                    String errorMsg = String.format("Unsupported AI engine provider: '%s'. Active options are: %s",
                            providerName, providerMap.keySet());
                    log.error(errorMsg);
                    return new IllegalArgumentException(errorMsg);
                });
    }
}
