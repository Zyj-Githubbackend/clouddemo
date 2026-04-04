package org.example.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.example.common.exception.BusinessException;
import org.example.config.MinioProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageService {

    private static final String IMAGE_WEBP = "image/webp";

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            IMAGE_WEBP
    );

    private final MinioProperties properties;

    public MinioStorageService(MinioProperties properties) {
        this.properties = properties;
    }

    public String uploadActivityImage(MultipartFile file) {
        ensureConfigured();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择图片文件");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP 格式图片");
        }

        long maxBytes = properties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片大小不能超过 " + properties.getMaxFileSizeMb() + " MB");
        }

        String objectKey = buildObjectKey(file.getOriginalFilename(), contentType);
        MinioClient client = buildClient();

        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists(client);
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            return objectKey;
        } catch (Exception ex) {
            throw new BusinessException("上传活动图片失败: " + ex.getMessage());
        }
    }

    public InputStream getObjectStream(String objectKey) {
        ensureConfigured();
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException("图片对象键不能为空");
        }

        try {
            return buildClient().getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new BusinessException("图片不存在");
            }
            throw new BusinessException("读取活动图片失败: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException("读取活动图片失败: " + ex.getMessage());
        }
    }

    public StatObjectResponse statObject(String objectKey) {
        ensureConfigured();
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException("图片对象键不能为空");
        }

        try {
            return buildClient().statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new BusinessException("图片不存在");
            }
            throw new BusinessException("读取图片信息失败: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException("读取图片信息失败: " + ex.getMessage());
        }
    }

    public String buildActivityImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }
        String encoded = URLEncoder.encode(imageKey, StandardCharsets.UTF_8);
        return properties.getPublicBaseUrl() + "/activity/image?objectKey=" + encoded;
    }

    public void deleteObjectQuietly(String objectKey) {
        if (!properties.isConfigured() || !StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            buildClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ignored) {
            // Ignore cleanup failures to avoid blocking business operations.
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new BusinessException("MinIO 未配置完整，请检查 MINIO_ENDPOINT、MINIO_ACCESS_KEY、MINIO_SECRET_KEY、MINIO_BUCKET");
        }
    }

    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    private void ensureBucketExists(MinioClient client) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build()
        );
        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build()
            );
        }
    }

    private String buildObjectKey(String originalFilename, String contentType) {
        String extension = resolveExtension(originalFilename, contentType);
        return "activities/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            if (ext.length() <= 10) {
                return ext.toLowerCase();
            }
        }
        return switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case MediaType.IMAGE_GIF_VALUE -> ".gif";
            case IMAGE_WEBP -> ".webp";
            default -> ".jpg";
        };
    }
}
