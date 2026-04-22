package org.example.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.common.exception.BusinessException;
import org.example.common.result.Result;
import org.example.dto.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final String userServiceName;

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${user.service.name:user-service}") String userServiceName) {
        this.restTemplate = restTemplate;
        this.userServiceName = userServiceName;
    }

    @Retry(name = "userServiceClient", fallbackMethod = "listUserSummariesByIdsFallback")
    @CircuitBreaker(name = "userServiceClient")
    @RateLimiter(name = "userServiceClient", fallbackMethod = "listUserSummariesByIdsFallback")
    @Bulkhead(name = "userServiceClient", type = Bulkhead.Type.SEMAPHORE)
    public Map<Long, UserSummary> listUserSummariesByIds(List<Long> userIds) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        URI uri = UriComponentsBuilder
                .fromUriString("http://" + userServiceName + "/internal/users/summaries")
                .queryParam("ids", normalizedUserIds.toArray())
                .build(true)
                .toUri();

        try {
            ResponseEntity<Result<List<UserSummary>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    }
            );
            Result<List<UserSummary>> result = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()
                    || result == null
                    || result.getCode() == null
                    || result.getCode() != 200) {
                throw new BusinessException("Failed to load user summaries from user-service");
            }

            List<UserSummary> summaries = result.getData();
            if (summaries == null || summaries.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<Long, UserSummary> summaryMap = new LinkedHashMap<>();
            for (UserSummary summary : summaries) {
                if (summary != null && summary.getId() != null) {
                    summaryMap.put(summary.getId(), summary);
                }
            }
            return summaryMap;
        } catch (RestClientException ex) {
            log.warn("failed to load user summaries from user-service userIds={}", normalizedUserIds, ex);
            throw new BusinessException("Failed to load user summaries from user-service");
        }
    }

    private Map<Long, UserSummary> listUserSummariesByIdsFallback(List<Long> userIds, Throwable throwable) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        log.warn("degraded user summary query due to resilience protection userIds={} reason={}",
                normalizedUserIds,
                throwable == null ? "unknown" : throwable.toString());
        return Collections.emptyMap();
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null && userId > 0) {
                normalized.add(userId);
            }
        }
        return normalized.stream().toList();
    }
}
