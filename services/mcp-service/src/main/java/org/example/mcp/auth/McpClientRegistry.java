package org.example.mcp.auth;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpClientRegistry {

    private final Map<String, McpClientRegistration> registrations = new ConcurrentHashMap<>();

    public McpClientRegistration register(String clientName, List<String> redirectUris) {
        String clientId = "cloud-demo-" + UUID.randomUUID();
        McpClientRegistration registration = new McpClientRegistration(
                clientId,
                clientName,
                List.copyOf(redirectUris),
                Instant.now()
        );
        registrations.put(clientId, registration);
        return registration;
    }

    public McpClientRegistration resolve(String clientId, String redirectUri) {
        McpClientRegistration registered = registrations.get(clientId);
        if (registered != null) {
            if (!registered.redirectUris().contains(redirectUri)) {
                throw new IllegalArgumentException("redirect_uri is not registered for this client");
            }
            return registered;
        }

        URI clientUri = parseUri(clientId, "client_id");
        if (clientUri.getScheme() == null || clientUri.getHost() == null) {
            throw new IllegalArgumentException("Unknown client_id");
        }

        return new McpClientRegistration(clientId, clientId, List.of(redirectUri), Instant.now());
    }

    private URI parseUri(String value, String fieldName) {
        try {
            return URI.create(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid URI", ex);
        }
    }
}
