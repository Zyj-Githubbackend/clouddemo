package org.example.mcp.auth;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.example.mcp.gateway.CloudDemoGatewayClient;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class AuthController {

    private final CloudDemoGatewayClient gatewayClient;
    private final McpClientRegistry clientRegistry;
    private final McpAuthorizationCodeStore authorizationCodeStore;
    private final McpJwtTokenService jwtTokenService;

    public AuthController(CloudDemoGatewayClient gatewayClient,
                          McpClientRegistry clientRegistry,
                          McpAuthorizationCodeStore authorizationCodeStore,
                          McpJwtTokenService jwtTokenService) {
        this.gatewayClient = gatewayClient;
        this.clientRegistry = clientRegistry;
        this.authorizationCodeStore = authorizationCodeStore;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping(value = {"/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> protectedResourceMetadata(HttpServletRequest request) {
        String baseUrl = externalBaseUrl(request);
        return Map.of(
                "resource", baseUrl + "/mcp",
                "authorization_servers", List.of(baseUrl),
                "scopes_supported", List.of("mcp", "mcp.admin"),
                "bearer_methods_supported", List.of("header")
        );
    }

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> authorizationServerMetadata(HttpServletRequest request) {
        String baseUrl = externalBaseUrl(request);
        return Map.of(
                "issuer", baseUrl,
                "authorization_endpoint", baseUrl + "/authorize",
                "token_endpoint", baseUrl + "/token",
                "registration_endpoint", baseUrl + "/register",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code"),
                "token_endpoint_auth_methods_supported", List.of("none"),
                "code_challenge_methods_supported", List.of("S256", "plain"),
                "scopes_supported", List.of("mcp", "mcp.admin"),
                "client_id_metadata_document_supported", true
        );
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> requestBody) {
        Object redirectUrisObject = requestBody.get("redirect_uris");
        if (!(redirectUrisObject instanceof List<?> rawUris) || rawUris.isEmpty()) {
            throw new IllegalArgumentException("redirect_uris is required");
        }

        List<String> redirectUris = rawUris.stream()
                .map(Object::toString)
                .peek(this::validateRedirectUri)
                .toList();

        String clientName = Objects.toString(requestBody.getOrDefault("client_name", "Codex MCP Client"), "Codex MCP Client");
        McpClientRegistration registration = clientRegistry.register(clientName, redirectUris);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", registration.clientId());
        response.put("client_id_issued_at", registration.clientIdIssuedAt().getEpochSecond());
        response.put("client_name", registration.clientName());
        response.put("redirect_uris", registration.redirectUris());
        response.put("grant_types", List.of("authorization_code"));
        response.put("response_types", List.of("code"));
        response.put("token_endpoint_auth_method", "none");

        return noStore(ResponseEntity.ok(response));
    }

    @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> authorizePage(@RequestParam Map<String, String> params) {
        validateAuthorizeParams(params);
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Cloud Demo MCP Login</title>
                  <style>
                    body { font-family: Arial, sans-serif; background: #f7f7f7; color: #222; }
                    .card { max-width: 420px; margin: 60px auto; background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,.08); }
                    h1 { margin-top: 0; font-size: 24px; }
                    label { display: block; margin-top: 12px; font-weight: 600; }
                    input { width: 100%%; box-sizing: border-box; margin-top: 6px; padding: 10px 12px; border: 1px solid #d0d5dd; border-radius: 8px; }
                    button { width: 100%%; margin-top: 18px; padding: 11px 14px; border: 0; border-radius: 8px; background: #111827; color: #fff; font-size: 15px; cursor: pointer; }
                    p { color: #555; line-height: 1.5; }
                    .meta { margin-top: 10px; font-size: 13px; color: #666; word-break: break-all; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Cloud Demo MCP Login</h1>
                    <p>Sign in with your existing platform account to authorize Codex.</p>
                    <form method="post" action="/authorize">
                      %s
                      <label>Username</label>
                      <input name="username" autocomplete="username" required />
                      <label>Password</label>
                      <input type="password" name="password" autocomplete="current-password" required />
                      <button type="submit">Authorize</button>
                    </form>
                    <div class="meta">Requested scope: %s</div>
                  </div>
                </body>
                </html>
                """.formatted(hiddenInputs(params), escapeHtml(params.getOrDefault("scope", "mcp")));

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authorizeSubmit(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> params = extractAuthorizeParams(form);
        validateAuthorizeParams(params);

        String username = requiredValue(form, "username");
        String password = requiredValue(form, "password");

        try {
            JsonNode data = gatewayClient.post("/user/login", new LoginRequest(username, password), null);
            String gatewayToken = data.path("token").asText(null);
            String role = data.path("userInfo").path("role").asText(null);
            String actualUsername = data.path("userInfo").path("username").asText(username);
            if (gatewayToken == null || gatewayToken.isBlank()) {
                throw new IllegalStateException("login did not return a token");
            }

            McpClientRegistration registration = clientRegistry.resolve(params.get("client_id"), params.get("redirect_uri"));
            McpAuthorizationCode code = authorizationCodeStore.issue(
                    registration.clientId(),
                    params.get("redirect_uri"),
                    params.get("code_challenge"),
                    params.getOrDefault("code_challenge_method", "plain"),
                    params.getOrDefault("scope", "mcp"),
                    gatewayToken,
                    actualUsername,
                    role
            );

            URI redirect = UriComponentsBuilder.fromUriString(params.get("redirect_uri"))
                    .queryParam("code", code.code())
                    .queryParam("state", params.get("state"))
                    .build(true)
                    .toUri();
            return ResponseEntity.status(302).location(redirect).build();
        } catch (Exception ex) {
            String html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head><meta charset="UTF-8"><title>Cloud Demo MCP Login</title></head>
                    <body style="font-family:Arial,sans-serif;background:#f7f7f7;color:#222;">
                      <div style="max-width:420px;margin:60px auto;background:#fff;padding:24px;border-radius:12px;box-shadow:0 10px 30px rgba(0,0,0,.08);">
                        <h1 style="margin-top:0;">Cloud Demo MCP Login</h1>
                        <div style="padding:10px 12px;border-radius:8px;background:#fef2f2;color:#991b1b;margin-bottom:12px;">%s</div>
                        <form method="post" action="/authorize">
                          %s
                          <label style="display:block;margin-top:12px;font-weight:600;">Username</label>
                          <input name="username" value="%s" autocomplete="username" required style="width:100%%;box-sizing:border-box;margin-top:6px;padding:10px 12px;border:1px solid #d0d5dd;border-radius:8px;" />
                          <label style="display:block;margin-top:12px;font-weight:600;">Password</label>
                          <input type="password" name="password" autocomplete="current-password" required style="width:100%%;box-sizing:border-box;margin-top:6px;padding:10px 12px;border:1px solid #d0d5dd;border-radius:8px;" />
                          <button type="submit" style="width:100%%;margin-top:18px;padding:11px 14px;border:0;border-radius:8px;background:#111827;color:#fff;font-size:15px;cursor:pointer;">Authorize</button>
                        </form>
                      </div>
                    </body>
                    </html>
                    """.formatted(escapeHtml(ex.getMessage()), hiddenInputs(params), escapeHtml(username));
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(html);
        }
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> token(@RequestParam MultiValueMap<String, String> form) {
        String grantType = requiredValue(form, "grant_type");
        if (!"authorization_code".equals(grantType)) {
            return oauthError("unsupported_grant_type", "Only authorization_code is supported");
        }

        String codeValue = requiredValue(form, "code");
        String clientId = requiredValue(form, "client_id");
        String redirectUri = requiredValue(form, "redirect_uri");
        String codeVerifier = requiredValue(form, "code_verifier");

        McpAuthorizationCode code = authorizationCodeStore.consume(codeValue);
        try {
            McpClientRegistration registration = clientRegistry.resolve(clientId, redirectUri);
            if (!registration.clientId().equals(code.clientId()) || !redirectUri.equals(code.redirectUri())) {
                return oauthError("invalid_grant", "authorization code does not match the client");
            }

            verifyCodeVerifier(codeVerifier, code.codeChallenge(), code.codeChallengeMethod());

            McpJwtClaims claims = jwtTokenService.parse(code.gatewayToken());
            long expiresIn = Math.max(60, Duration.between(Instant.now(), claims.expiresAt()).getSeconds());
            String scope = normalizeScope(code.scope(), claims.role());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", code.gatewayToken());
            response.put("token_type", "Bearer");
            response.put("expires_in", expiresIn);
            response.put("scope", scope);
            return noStore(ResponseEntity.ok(response));
        } catch (Exception ex) {
            return oauthError("invalid_grant", ex.getMessage());
        }
    }

    @PostMapping(value = "/mcp/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> helperLogin(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new IllegalArgumentException("username and password are required");
        }

        JsonNode data = gatewayClient.post("/user/login", request, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", data.path("token").asText(null));
        result.put("role", data.path("userInfo").path("role").asText(null));
        result.put("username", data.path("userInfo").path("username").asText(null));
        result.put("mcpEndpoint", "/mcp");
        return result;
    }

    private ResponseEntity<Map<String, Object>> noStore(ResponseEntity<Map<String, Object>> responseEntity) {
        return ResponseEntity.status(responseEntity.getStatusCode())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
    }

    private ResponseEntity<Map<String, Object>> oauthError(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return noStore(ResponseEntity.badRequest().body(body));
    }

    private Map<String, String> extractAuthorizeParams(MultiValueMap<String, String> form) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String name : List.of("response_type", "client_id", "redirect_uri", "state", "scope", "code_challenge", "code_challenge_method", "resource")) {
            String value = form.getFirst(name);
            if (value != null) {
                params.put(name, value);
            }
        }
        return params;
    }

    private void validateAuthorizeParams(Map<String, String> params) {
        if (!"code".equals(params.get("response_type"))) {
            throw new IllegalArgumentException("response_type must be code");
        }

        String clientId = params.get("client_id");
        String redirectUri = params.get("redirect_uri");
        String codeChallenge = params.get("code_challenge");

        if (isBlank(clientId) || isBlank(redirectUri) || isBlank(codeChallenge)) {
            throw new IllegalArgumentException("client_id, redirect_uri, and code_challenge are required");
        }

        validateRedirectUri(redirectUri);
        clientRegistry.resolve(clientId, redirectUri);

        String method = params.getOrDefault("code_challenge_method", "plain");
        if (!"plain".equals(method) && !"S256".equals(method)) {
            throw new IllegalArgumentException("Unsupported code_challenge_method");
        }
    }

    private void validateRedirectUri(String redirectUri) {
        URI uri;
        try {
            uri = URI.create(redirectUri);
        } catch (Exception ex) {
            throw new IllegalArgumentException("redirect_uri must be a valid URI", ex);
        }

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("redirect_uri must not contain a fragment");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        boolean localHttp = "http".equalsIgnoreCase(scheme)
                && host != null
                && (host.equals("127.0.0.1") || host.equals("localhost"));
        boolean secure = "https".equalsIgnoreCase(scheme);
        if (!localHttp && !secure) {
            throw new IllegalArgumentException("redirect_uri must use https or localhost http");
        }
    }

    private void verifyCodeVerifier(String codeVerifier, String codeChallenge, String method) {
        if ("S256".equalsIgnoreCase(method)) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String actual = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
                if (!actual.equals(codeChallenge)) {
                    throw new IllegalArgumentException("code_verifier does not match code_challenge");
                }
                return;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to verify code_verifier", ex);
            }
        }

        if (!Objects.equals(codeVerifier, codeChallenge)) {
            throw new IllegalArgumentException("code_verifier does not match code_challenge");
        }
    }

    private String hiddenInputs(Map<String, String> params) {
        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type=\"hidden\" name=\"")
                    .append(escapeHtml(entry.getKey()))
                    .append("\" value=\"")
                    .append(escapeHtml(entry.getValue()))
                    .append("\" />");
        }
        return html.toString();
    }

    private String normalizeScope(String requestedScope, String role) {
        if ("ADMIN".equals(role)) {
            return isBlank(requestedScope) ? "mcp mcp.admin" : requestedScope + " mcp.admin";
        }
        return isBlank(requestedScope) ? "mcp" : requestedScope;
    }

    private String requiredValue(MultiValueMap<String, String> form, String key) {
        String value = form.getFirst(key);
        if (isBlank(value)) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String externalBaseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
