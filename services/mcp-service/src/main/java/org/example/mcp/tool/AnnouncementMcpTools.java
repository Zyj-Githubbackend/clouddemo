package org.example.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.mcp.auth.McpRequestContext;
import org.example.mcp.gateway.CloudDemoGatewayClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnnouncementMcpTools {

    private final CloudDemoGatewayClient gatewayClient;
    private final McpRequestContext requestContext;

    public AnnouncementMcpTools(CloudDemoGatewayClient gatewayClient, McpRequestContext requestContext) {
        this.gatewayClient = gatewayClient;
        this.requestContext = requestContext;
    }

    @Tool(description = "List homepage announcements. Use this when the user asks for current platform announcements, notices, or the default homepage content. The limit defaults to 5 and is capped at 20. Returns announcement summaries including image URLs and optional linked activityId. Requires a logged-in MCP session.")
    public List<Map<String, Object>> listHomeAnnouncements(Integer limit) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("limit", String.valueOf(limit == null || limit < 1 ? 5 : Math.min(limit, 20)));

        JsonNode data = gatewayClient.get("/announcement/home", params, requestContext.requireGatewayToken());
        return toAnnouncementList(data);
    }

    @Tool(description = "List published announcements. Use this when the user wants to browse announcements with pagination. Supports page and size; size is capped at 50. Returns paging metadata and announcement summaries including image URLs and optional linked activityId. Requires a logged-in MCP session.")
    public Map<String, Object> listAnnouncements(Integer page, Integer size) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page == null || page < 1 ? 1 : page));
        params.add("size", String.valueOf(size == null || size < 1 ? 10 : Math.min(size, 50)));

        JsonNode data = gatewayClient.get("/announcement/list", params, requestContext.requireGatewayToken());
        return toPagedAnnouncements(data, page, size);
    }

    @Tool(description = "Get a published announcement detail by announcementId. Use this when the user asks to read a specific announcement, including its content, images, status, publish time, and optional linked activityId. Requires a logged-in MCP session.")
    public Map<String, Object> getAnnouncementDetail(Long announcementId) {
        requireId(announcementId, "announcementId");
        JsonNode data = gatewayClient.get("/announcement/" + announcementId, null, requestContext.requireGatewayToken());
        return toAnnouncement(data);
    }

    @Tool(description = "List announcements for administrator management. Use this when an admin wants to review all announcements, including offline ones. Supports page, size, and optional status such as PUBLISHED or OFFLINE. Returns paging metadata and announcement records. Requires an admin MCP session.")
    public Map<String, Object> listAdminAnnouncements(Integer page, Integer size, String status) {
        requestContext.requireAdmin();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page == null || page < 1 ? 1 : page));
        params.add("size", String.valueOf(size == null || size < 1 ? 10 : Math.min(size, 50)));
        addIfHasText(params, "status", status);

        JsonNode data = gatewayClient.get("/announcement/admin/list", params, requestContext.requireGatewayToken());
        return toPagedAnnouncements(data, page, size);
    }

    @Tool(description = "Get an announcement detail for administrator management, including offline records. Use this before editing or auditing a specific announcement. Requires an admin MCP session.")
    public Map<String, Object> getAdminAnnouncementDetail(Long announcementId) {
        requestContext.requireAdmin();
        requireId(announcementId, "announcementId");

        JsonNode data = gatewayClient.get("/announcement/admin/" + announcementId, null, requestContext.requireGatewayToken());
        return toAnnouncement(data);
    }

    @Tool(description = "Create a new announcement as an administrator. Use this only after the title and content are known. Optional fields include activityId, status, sortOrder, imageKey, and imageKeysCsv. status defaults in the backend when omitted, and imageKeysCsv accepts multiple image keys separated by commas. Returns created=true and the submitted title. Requires an admin MCP session.")
    public Map<String, Object> createAnnouncement(
            String title,
            String content,
            Long activityId,
            String status,
            Integer sortOrder,
            String imageKey,
            String imageKeysCsv
    ) {
        requestContext.requireAdmin();

        Map<String, Object> request = buildAnnouncementRequest(
                title,
                content,
                activityId,
                status,
                sortOrder,
                imageKey,
                imageKeysCsv
        );

        gatewayClient.post("/announcement/admin", request, requestContext.requireGatewayToken());
        return Map.of("created", true, "title", title);
    }

    @Tool(description = "Update an existing announcement as an administrator. announcementId is required. Provide the new title, content, optional linked activityId, status, sortOrder, imageKey, and imageKeysCsv. imageKeysCsv accepts multiple image keys separated by commas. Returns updated=true and the announcementId. Requires an admin MCP session.")
    public Map<String, Object> updateAnnouncement(
            Long announcementId,
            String title,
            String content,
            Long activityId,
            String status,
            Integer sortOrder,
            String imageKey,
            String imageKeysCsv
    ) {
        requestContext.requireAdmin();
        requireId(announcementId, "announcementId");

        Map<String, Object> request = buildAnnouncementRequest(
                title,
                content,
                activityId,
                status,
                sortOrder,
                imageKey,
                imageKeysCsv
        );

        gatewayClient.put("/announcement/admin/" + announcementId, request, requestContext.requireGatewayToken());
        return Map.of("updated", true, "announcementId", announcementId);
    }

    @Tool(description = "Publish an existing announcement as an administrator. Use this to bring an offline announcement back online. announcementId is required. Returns published=true and the announcementId. Requires an admin MCP session.")
    public Map<String, Object> publishAnnouncement(Long announcementId) {
        requestContext.requireAdmin();
        requireId(announcementId, "announcementId");

        gatewayClient.post("/announcement/admin/" + announcementId + "/publish", Map.of(), requestContext.requireGatewayToken());
        return Map.of("published", true, "announcementId", announcementId);
    }

    @Tool(description = "Take an existing announcement offline as an administrator. Use this when an announcement should no longer appear on the homepage or published list but should remain in records. announcementId is required. Returns offline=true and the announcementId. Requires an admin MCP session.")
    public Map<String, Object> offlineAnnouncement(Long announcementId) {
        requestContext.requireAdmin();
        requireId(announcementId, "announcementId");

        gatewayClient.post("/announcement/admin/" + announcementId + "/offline", Map.of(), requestContext.requireGatewayToken());
        return Map.of("offline", true, "announcementId", announcementId);
    }

    @Tool(description = "Delete an announcement as an administrator. Use this only when the announcement should be permanently removed rather than taken offline. announcementId is required. Returns deleted=true and the announcementId. Requires an admin MCP session.")
    public Map<String, Object> deleteAnnouncement(Long announcementId) {
        requestContext.requireAdmin();
        requireId(announcementId, "announcementId");

        gatewayClient.delete("/announcement/admin/" + announcementId, requestContext.requireGatewayToken());
        return Map.of("deleted", true, "announcementId", announcementId);
    }

    @Tool(description = "Upload an announcement image as an administrator. Use this before createAnnouncement or updateAnnouncement when an image must first be stored by the backend. Inputs are filename, contentType, and base64Content. base64Content may be raw Base64 or a data URL. Supports JPG, PNG, GIF, and WEBP. Returns imageKey, imageUrl, normalized filename, contentType, and byteLength. Requires an admin MCP session.")
    public Map<String, Object> uploadAnnouncementImage(
            String filename,
            String contentType,
            String base64Content
    ) {
        requestContext.requireAdmin();
        requireText(base64Content, "base64Content");

        ParsedBase64File parsedFile = parseBase64File(filename, contentType, base64Content);
        JsonNode data = gatewayClient.postMultipart(
                "/announcement/admin/image",
                "file",
                parsedFile.content(),
                parsedFile.filename(),
                parsedFile.contentType(),
                requestContext.requireGatewayToken()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageKey", textOrNull(data, "imageKey"));
        result.put("imageUrl", textOrNull(data, "imageUrl"));
        result.put("filename", parsedFile.filename());
        result.put("contentType", parsedFile.contentType());
        result.put("byteLength", parsedFile.content().length);
        return result;
    }

    private Map<String, Object> buildAnnouncementRequest(
            String title,
            String content,
            Long activityId,
            String status,
            Integer sortOrder,
            String imageKey,
            String imageKeysCsv
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("content", content);
        request.put("activityId", activityId);
        request.put("status", status);
        request.put("sortOrder", sortOrder);
        request.put("imageKey", imageKey);
        request.put("imageKeys", splitCsv(imageKeysCsv));
        return request;
    }

    private Map<String, Object> toPagedAnnouncements(JsonNode data, Integer requestedPage, Integer requestedSize) {
        List<Map<String, Object>> announcements = new ArrayList<>();
        for (JsonNode item : data.path("records")) {
            announcements.add(toAnnouncement(item));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", data.path("current").asInt(requestedPage == null ? 1 : requestedPage));
        result.put("size", data.path("size").asInt(requestedSize == null ? 10 : requestedSize));
        result.put("total", data.path("total").asLong());
        result.put("pages", data.path("pages").asLong());
        result.put("announcements", announcements);
        return result;
    }

    private List<Map<String, Object>> toAnnouncementList(JsonNode data) {
        List<Map<String, Object>> announcements = new ArrayList<>();
        for (JsonNode item : data) {
            announcements.add(toAnnouncement(item));
        }
        return announcements;
    }

    private Map<String, Object> toAnnouncement(JsonNode item) {
        Map<String, Object> announcement = new LinkedHashMap<>();
        announcement.put("id", item.path("id").asLong());
        announcement.put("title", textOrNull(item, "title"));
        announcement.put("content", textOrNull(item, "content"));
        announcement.put("imageKey", textOrNull(item, "imageKey"));
        announcement.put("imageUrl", textOrNull(item, "imageUrl"));
        announcement.put("imageKeys", toStringList(item.path("imageKeys")));
        announcement.put("imageUrls", toStringList(item.path("imageUrls")));
        announcement.put("activityId", item.hasNonNull("activityId") ? item.path("activityId").asLong() : null);
        announcement.put("status", textOrNull(item, "status"));
        announcement.put("sortOrder", item.hasNonNull("sortOrder") ? item.path("sortOrder").asInt() : null);
        announcement.put("publisherId", item.hasNonNull("publisherId") ? item.path("publisherId").asLong() : null);
        announcement.put("publishTime", textOrNull(item, "publishTime"));
        announcement.put("createTime", textOrNull(item, "createTime"));
        announcement.put("updateTime", textOrNull(item, "updateTime"));
        return announcement;
    }

    private void addIfHasText(MultiValueMap<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key, value.trim());
        }
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : csv.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private void requireId(Long id, String fieldName) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asText() : null;
    }

    private List<String> toStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (!item.isNull()) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private ParsedBase64File parseBase64File(String filename, String contentType, String base64Content) {
        String actualFilename = filename == null || filename.isBlank() ? "announcement-image" : filename.trim();
        String actualContentType = contentType == null ? "" : contentType.trim();
        String actualBase64 = base64Content.trim();

        if (actualBase64.startsWith("data:")) {
            int semicolonIndex = actualBase64.indexOf(';');
            int commaIndex = actualBase64.indexOf(',');
            if (semicolonIndex > 5 && commaIndex > semicolonIndex) {
                if (actualContentType.isBlank()) {
                    actualContentType = actualBase64.substring(5, semicolonIndex);
                }
                actualBase64 = actualBase64.substring(commaIndex + 1);
            }
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(actualBase64);
            return new ParsedBase64File(
                    ensureFilenameExtension(actualFilename, actualContentType),
                    actualContentType.isBlank() ? "application/octet-stream" : actualContentType,
                    decoded
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("base64Content is not valid Base64", ex);
        }
    }

    private String ensureFilenameExtension(String filename, String contentType) {
        if (filename.contains(".")) {
            return filename;
        }
        return filename + switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private record ParsedBase64File(String filename, String contentType, byte[] content) {
    }
}
