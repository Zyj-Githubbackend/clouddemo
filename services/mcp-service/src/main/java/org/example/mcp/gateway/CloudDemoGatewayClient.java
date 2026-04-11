package org.example.mcp.gateway;

import com.fasterxml.jackson.databind.JsonNode;
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

@Component
public class CloudDemoGatewayClient {

    private final RestTemplate restTemplate;
    private final CloudDemoApiProperties properties;

    public CloudDemoGatewayClient(RestTemplate restTemplate, CloudDemoApiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public JsonNode get(String path, MultiValueMap<String, String> queryParams, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
                .queryParams(queryParams != null ? queryParams : new LinkedMultiValueMap<>())
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.GET, null, bearerToken);
    }

    public JsonNode post(String path, Object body, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.POST, body, bearerToken);
    }

    public JsonNode put(String path, Object body, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.PUT, body, bearerToken);
    }

    public JsonNode delete(String path, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
                .build(true)
                .toUri();

        return exchange(uri, HttpMethod.DELETE, null, bearerToken);
    }

    public JsonNode postMultipart(String path,
                                  String partName,
                                  byte[] content,
                                  String filename,
                                  String contentType,
                                  String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
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

    public GatewayBinaryResponse getBinary(String path, MultiValueMap<String, String> queryParams, String bearerToken) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
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
