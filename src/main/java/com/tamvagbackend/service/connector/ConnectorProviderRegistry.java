package com.tamvagbackend.service.connector;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectorProviderRegistry {

    private final Map<String, ConnectorProvider> providers;

    public ConnectorProviderRegistry(List<ConnectorProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.getProviderCode().toUpperCase(),
                        Function.identity()
                ));
    }

    public ConnectorProvider getProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("Provider code is required");
        }

        ConnectorProvider provider =
                providers.get(providerCode.trim().toUpperCase());

        if (provider == null) {
            throw new IllegalArgumentException(
                    "No connector provider registered for: " + providerCode
            );
        }

        return provider;
    }
}