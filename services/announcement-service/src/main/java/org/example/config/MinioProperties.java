package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket = "activity-images";

    private String publicBaseUrl = "/api";

    private long maxFileSizeMb = 5;

    public boolean isConfigured() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(accessKey)
                && StringUtils.hasText(secretKey)
                && StringUtils.hasText(bucket);
    }
}
