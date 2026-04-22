package org.example.mcp.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.mcp.config.CloudDemoApiProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CloudDemoGatewayClient {

    private final RestTemplate restTemplate;
    private final CloudDemoApiProperties properties;

    public CloudDemoGatewayClient(RestTemplate restTemplate, CloudDemoApiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackGet")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public JsonNode get(String path, MultiValueMap<String, String> queryParams, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .queryParams(queryParams != null ? queryParams : new LinkedMultiValueMap<>())
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.GET, null, bearerToken);
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackPost")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public JsonNode post(String path, Object body, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.POST, body, bearerToken);
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackPut")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public JsonNode put(String path, Object body, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.PUT, body, bearerToken);
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackDelete")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public JsonNode delete(String path, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.DELETE, null, bearerToken);
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackPostMultipart")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public JsonNode postMultipart(String path,
                                  String partName,
                                  byte[] content,
                                  String filename,
                                  String contentType,
                                  String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(stripBearerPrefix(bearerToken));
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        NamedByteArrayResource resource = new NamedByteArrayResource(content, filename);
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentDispositionFormData(partName, filename);
        partHeaders.setContentType(MediaType.parseMediaType(
                contentType == null || contentType.isBlank()
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : contentType
        ));
        body.add(partName, new HttpEntity<>(resource, partHeaders));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.POST, entity, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            if (bodyNode == null) {
                throw new IllegalStateException("Gateway response body is empty");
            }

            int code = bodyNode.path("code").asInt(500);
            if (code != 200) {
                throw new IllegalStateException(bodyNode.path("message").asText("Gateway call failed"));
            }

            return bodyNode.path("data");
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    responseBody == null || responseBody.isBlank()
                            ? "Gateway call failed with status " + ex.getStatusCode().value()
                            : responseBody,
                    ex
            );
        }
    }

    @Retry(name = "gatewayClient", fallbackMethod = "fallbackGetBinary")
    @CircuitBreaker(name = "gatewayClient")
    @Bulkhead(name = "gatewayClient", type = Bulkhead.Type.SEMAPHORE)
    public GatewayBinaryResponse getBinary(String path, MultiValueMap<String, String> queryParams, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(normalizePath(path))
                .queryParams(queryParams != null ? queryParams : new LinkedMultiValueMap<>())
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes(MediaType.ALL_VALUE));
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(stripBearerPrefix(bearerToken));
        }

        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);
            byte[] content = response.getBody();
            if (content == null) {
                throw new IllegalStateException("Gateway binary response body is empty");
            }

            MediaType mediaType = response.getHeaders().getContentType();
            ContentDisposition disposition = response.getHeaders().getContentDisposition();
            return new GatewayBinaryResponse(
                    content,
                    mediaType != null ? mediaType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    disposition != null ? disposition.getFilename() : null
            );
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    responseBody == null || responseBody.isBlank()
                            ? "Gateway call failed with status " + ex.getStatusCode().value()
                            : responseBody,
                    ex
            );
        }
    }

    private JsonNode exchange(URI uri, HttpMethod method, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(stripBearerPrefix(bearerToken));
        }

        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(uri, method, entity, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            if (bodyNode == null) {
                throw new IllegalStateException("Gateway response body is empty");
            }

            int code = bodyNode.path("code").asInt(500);
            if (code != 200) {
                throw new IllegalStateException(bodyNode.path("message").asText("Gateway call failed"));
            }

            return bodyNode.path("data");
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    responseBody == null || responseBody.isBlank()
                            ? "Gateway call failed with status " + ex.getStatusCode().value()
                            : responseBody,
                    ex
            );
        }
    }

    private String stripBearerPrefix(String bearerToken) {
        if (bearerToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return bearerToken.substring(7).trim();
        }
        return bearerToken.trim();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "/api";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.startsWith("/api/")) {
            return normalized;
        }
        if ("/api".equals(normalized)) {
            return normalized;
        }
        return "/api" + normalized;
    }

    private JsonNode fallbackGet(String path,
                                 MultiValueMap<String, String> queryParams,
                                 String bearerToken,
                                 Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("queryParams", queryParams == null ? Map.of() : queryParams);
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("GET", path, throwable, details);
    }

    private JsonNode fallbackPost(String path, Object body, String bearerToken, Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("hasBody", body != null);
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("POST", path, throwable, details);
    }

    private JsonNode fallbackPut(String path, Object body, String bearerToken, Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("hasBody", body != null);
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("PUT", path, throwable, details);
    }

    private JsonNode fallbackDelete(String path, String bearerToken, Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("DELETE", path, throwable, details);
    }

    private JsonNode fallbackPostMultipart(String path,
                                           String partName,
                                           byte[] content,
                                           String filename,
                                           String contentType,
                                           String bearerToken,
                                           Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("partName", partName);
        details.put("filename", filename);
        details.put("contentLength", content == null ? 0 : content.length);
        details.put("contentType", contentType);
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("POST_MULTIPART", path, throwable, details);
    }

    private GatewayBinaryResponse fallbackGetBinary(String path,
                                                    MultiValueMap<String, String> queryParams,
                                                    String bearerToken,
                                                    Throwable throwable) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("queryParams", queryParams == null ? Map.of() : queryParams);
        details.put("hasBearerToken", bearerToken != null && !bearerToken.isBlank());
        throw gatewayUnavailable("GET_BINARY", path, throwable, details);
    }

    private IllegalStateException gatewayUnavailable(String method,
                                                     String path,
                                                     Throwable throwable,
                                                     Map<String, Object> details) {
        String normalizedPath = normalizePath(path);
        String message = "Gateway call unavailable after resilience protections method=" + method + " path=" + normalizedPath;
        return new IllegalStateException(message + " details=" + details, throwable);
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public long contentLength() {
            return getByteArray().length;
        }
    }
}
