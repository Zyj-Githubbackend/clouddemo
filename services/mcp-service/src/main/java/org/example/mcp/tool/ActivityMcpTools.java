package org.example.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.mcp.auth.McpRequestContext;
import org.example.mcp.gateway.CloudDemoGatewayClient;
import org.example.mcp.gateway.GatewayBinaryResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActivityMcpTools {

    private final CloudDemoGatewayClient gatewayClient;
    private final McpRequestContext requestContext;

    public ActivityMcpTools(CloudDemoGatewayClient gatewayClient, McpRequestContext requestContext) {
        this.gatewayClient = gatewayClient;
        this.requestContext = requestContext;
    }

    @Tool(description = "List volunteer activities visible to the current user. Use this for browsing, searching, or filtering activities. Supports pagination with page and size, plus optional filters: status, category, and recruitmentPhase. Returns paging metadata and an activities array with summary fields such as id, title, category, status, location, volunteerHours, activity times, participant counts, availableSlots, registration state, and imageUrls. Requires a logged-in MCP session.")
    public Map<String, Object> listActivities(
            Integer page,
            Integer size,
            String status,
            String category,
            String recruitmentPhase
    ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page == null || page < 1 ? 1 : page));
        params.add("size", String.valueOf(size == null || size < 1 ? 10 : Math.min(size, 50)));
        addIfHasText(params, "status", status);
        addIfHasText(params, "category", category);
        addIfHasText(params, "recruitmentPhase", recruitmentPhase);

        JsonNode data = gatewayClient.get("/activity/list", params, requestContext.requireGatewayToken());
        List<Map<String, Object>> activities = new ArrayList<>();
        for (JsonNode item : data.path("records")) {
            activities.add(toActivitySummary(item));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", data.path("current").asInt(page == null ? 1 : page));
        result.put("size", data.path("size").asInt(size == null ? 10 : size));
        result.put("total", data.path("total").asLong());
        result.put("pages", data.path("pages").asLong());
        result.put("activities", activities);
        return result;
    }

    @Tool(description = "Get the full detail of a volunteer activity by activityId. Use this when the user asks about a specific activity, including its description, images, schedule, location, capacity, status, and whether the current user is already registered. Returns the full activity detail object with both summary fields and detail fields such as description, imageKeys, imageKey, and imageUrl. Requires a logged-in MCP session.")
    public Map<String, Object> getActivityDetail(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId is required");
        }

        JsonNode data = gatewayClient.get("/activity/" + activityId, null, requestContext.requireGatewayToken());
        return toActivityDetail(data);
    }

    @Tool(description = "Get all activity registrations of the currently logged-in user. Use this for queries like my registrations, my sign-ups, my volunteer history, or checking sign-in and hour-confirmation progress. Returns a list of registration records including registration id, activityId, activityTitle, location, volunteerHours, startTime, registrationTime, checkInStatus, checkInTime, hoursConfirmed, confirmTime, and status. Requires a logged-in MCP session.")
    public List<Map<String, Object>> getMyRegistrations() {
        JsonNode data = gatewayClient.get("/activity/myRegistrations", null, requestContext.requireGatewayToken());

        List<Map<String, Object>> registrations = new ArrayList<>();
        for (JsonNode item : data) {
            registrations.add(toRegistration(item));
        }
        return registrations;
    }

    @Tool(description = "Register the currently logged-in user for a volunteer activity. Use this only when the user explicitly asks to sign up for an activity and you already know the target activityId. Returns a confirmation object with registered=true and the activityId. Requires a logged-in MCP session.")
    public Map<String, Object> registerActivity(Long activityId) {
        requireId(activityId, "activityId");
        gatewayClient.post("/activity/register/" + activityId, Map.of(), requestContext.requireGatewayToken());
        return Map.of("registered", true, "activityId", activityId);
    }

    @Tool(description = "Cancel the currently logged-in user's own registration for a volunteer activity. Use this only when the user explicitly asks to withdraw from an activity they previously joined. activityId is required. The backend may reject cancellations after the activity starts or after check-in. Returns cancelled=true and the activityId on success. Requires a logged-in MCP session.")
    public Map<String, Object> cancelMyRegistration(Long activityId) {
        requireId(activityId, "activityId");
        gatewayClient.post("/activity/cancelRegistration/" + activityId, Map.of(), requestContext.requireGatewayToken());
        return Map.of("cancelled", true, "activityId", activityId);
    }

    @Tool(description = "Generate a Chinese volunteer-activity description draft for administrators through the backend AI endpoint. Use this before creating or editing an activity when the admin needs help writing polished Chinese copy. Typical inputs are location, category, keywords, and volunteerHours. Returns a single description string. Requires an admin MCP session.")
    public Map<String, Object> generateActivityDescription(
            String location,
            String category,
            String keywords,
            BigDecimal volunteerHours
    ) {
        requestContext.requireAdmin();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("location", location);
        request.put("category", category);
        request.put("keywords", keywords);
        request.put("volunteerHours", volunteerHours);

        JsonNode data = gatewayClient.post("/activity/ai/generate", request, requestContext.requireGatewayToken());
        return Map.of("description", data.asText());
    }

    @Tool(description = "Create a new volunteer activity as an administrator. Use this only after all required event details are known. Expected inputs include title, description, location, maxParticipants, volunteerHours, startTime, endTime, registrationStartTime, registrationDeadline, and category. imageKey can hold a legacy single image key, while imageKeysCsv can contain multiple image keys separated by commas. Returns created=true and the title on success. Requires an admin MCP session.")
    public Map<String, Object> createActivity(
            String title,
            String description,
            String location,
            Integer maxParticipants,
            BigDecimal volunteerHours,
            String startTime,
            String endTime,
            String registrationStartTime,
            String registrationDeadline,
            String category,
            String imageKey,
            String imageKeysCsv
    ) {
        requestContext.requireAdmin();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("description", description);
        request.put("location", location);
        request.put("maxParticipants", maxParticipants);
        request.put("volunteerHours", volunteerHours);
        request.put("startTime", startTime);
        request.put("endTime", endTime);
        request.put("registrationStartTime", registrationStartTime);
        request.put("registrationDeadline", registrationDeadline);
        request.put("category", category);
        request.put("imageKey", imageKey);
        request.put("imageKeys", splitCsv(imageKeysCsv));

        gatewayClient.post("/activity/create", request, requestContext.requireGatewayToken());
        return Map.of("created", true, "title", title);
    }

    @Tool(description = "Update an existing volunteer activity as an administrator. Use this when the admin wants to edit an activity's content, schedule, location, participant limit, category, or images. activityId is required. imageKeysCsv accepts multiple image keys separated by commas. Returns updated=true and the activityId on success. Requires an admin MCP session.")
    public Map<String, Object> updateActivity(
            Long activityId,
            String title,
            String description,
            String location,
            Integer maxParticipants,
            BigDecimal volunteerHours,
            String startTime,
            String endTime,
            String registrationStartTime,
            String registrationDeadline,
            String category,
            String imageKey,
            String imageKeysCsv
    ) {
        requestContext.requireAdmin();
        requireId(activityId, "activityId");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("description", description);
        request.put("location", location);
        request.put("maxParticipants", maxParticipants);
        request.put("volunteerHours", volunteerHours);
        request.put("startTime", startTime);
        request.put("endTime", endTime);
        request.put("registrationStartTime", registrationStartTime);
        request.put("registrationDeadline", registrationDeadline);
        request.put("category", category);
        request.put("imageKey", imageKey);
        request.put("imageKeys", splitCsv(imageKeysCsv));

        gatewayClient.put("/activity/" + activityId, request, requestContext.requireGatewayToken());
        return Map.of("updated", true, "activityId", activityId);
    }

    @Tool(description = "Upload an activity image as an administrator. Use this before createActivity or updateActivity when an image must first be stored by the backend. Inputs are filename, contentType, and base64Content. base64Content may be raw Base64 or a data URL. Supports JPG, PNG, GIF, and WEBP. Returns imageKey, imageUrl, normalized filename, contentType, and byteLength. Requires an admin MCP session.")
    public Map<String, Object> uploadActivityImage(
            String filename,
            String contentType,
            String base64Content
    ) {
        requestContext.requireAdmin();
        requireText(base64Content, "base64Content");

        ParsedBase64File parsedFile = parseBase64File(filename, contentType, base64Content);
        JsonNode data = gatewayClient.postMultipart(
                "/activity/admin/image",
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

    @Tool(description = "Cancel a volunteer activity as an administrator. Use this when an existing activity should no longer proceed but still needs to remain in system records as cancelled. activityId is required. Returns cancelled=true and the activityId. Requires an admin MCP session.")
    public Map<String, Object> cancelActivity(Long activityId) {
        requestContext.requireAdmin();
        requireId(activityId, "activityId");
        gatewayClient.post("/activity/" + activityId + "/cancel", Map.of(), requestContext.requireGatewayToken());
        return Map.of("cancelled", true, "activityId", activityId);
    }

    @Tool(description = "Mark a volunteer activity as completed as an administrator. Use this after an activity has finished and should move into the completed state for later hour confirmation workflows. activityId is required. Returns completed=true and the activityId. Requires an admin MCP session.")
    public Map<String, Object> completeActivity(Long activityId) {
        requestContext.requireAdmin();
        requireId(activityId, "activityId");
        gatewayClient.post("/activity/" + activityId + "/complete", Map.of(), requestContext.requireGatewayToken());
        return Map.of("completed", true, "activityId", activityId);
    }

    @Tool(description = "Delete a volunteer activity as an administrator. Use this only when the activity should be permanently removed rather than updated, cancelled, or completed. activityId is required. Returns deleted=true and the activityId. Requires an admin MCP session.")
    public Map<String, Object> deleteActivity(Long activityId) {
        requestContext.requireAdmin();
        requireId(activityId, "activityId");
        gatewayClient.delete("/activity/" + activityId, requestContext.requireGatewayToken());
        return Map.of("deleted", true, "activityId", activityId);
    }

    @Tool(description = "List activity registration records for administrators. Use this when an admin needs the participant roster for one activity or wants to inspect registrations across activities. The optional activityId filter narrows the results to one activity. Returns registration records enriched with user identity fields such as userId, username, realName, studentNo, and phone, plus registration and check-in status fields. Requires an admin MCP session.")
    public List<Map<String, Object>> listAdminRegistrations(Long activityId) {
        requestContext.requireAdmin();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (activityId != null) {
            params.add("activityId", String.valueOf(activityId));
        }

        JsonNode data = gatewayClient.get("/activity/admin/registrations", params, requestContext.requireGatewayToken());
        List<Map<String, Object>> registrations = new ArrayList<>();
        for (JsonNode item : data) {
            registrations.add(toAdminRegistration(item));
        }
        return registrations;
    }

    @Tool(description = "List activities that have already ended and are suitable for volunteer-hour confirmation workflows. Use this to help an admin choose which finished activity to process next. Returns activity summaries. Requires an admin MCP session.")
    public List<Map<String, Object>> listEndedActivities() {
        requestContext.requireAdmin();
        JsonNode data = gatewayClient.get("/activity/admin/endedActivities", null, requestContext.requireGatewayToken());
        return toActivityList(data);
    }

    @Tool(description = "List activities that are currently available for administrator check-in workflows. Use this when an admin needs to find activities whose participants can be checked in now. Returns activity summaries. Requires an admin MCP session.")
    public List<Map<String, Object>> listCheckInActivities() {
        requestContext.requireAdmin();
        JsonNode data = gatewayClient.get("/activity/admin/checkInActivities", null, requestContext.requireGatewayToken());
        return toActivityList(data);
    }

    @Tool(description = "List pending check-in registrations for administrators. Use this when an admin wants to see which participants of a specific activity have not yet checked in. activityId is required. Returns only active registration records whose checkInStatus is not yet completed. Each record includes registration identity, volunteer info, and activity info. Requires an admin MCP session.")
    public List<Map<String, Object>> listPendingCheckInRegistrations(Long activityId) {
        requestContext.requireAdmin();
        requireId(activityId, "activityId");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("activityId", String.valueOf(activityId));

        JsonNode data = gatewayClient.get("/activity/admin/registrations", params, requestContext.requireGatewayToken());
        List<Map<String, Object>> registrations = new ArrayList<>();
        for (JsonNode item : data) {
            Integer checkInStatus = intOrNull(item, "checkInStatus");
            if (checkInStatus == null || checkInStatus != 1) {
                registrations.add(toAdminRegistration(item));
            }
        }
        return registrations;
    }

    @Tool(description = "Mark a specific registration as checked in as an administrator. Use this after identifying the correct registrationId from listAdminRegistrations and only when the participant has actually arrived or been verified. Returns checkedIn=true and the registrationId. Requires an admin MCP session.")
    public Map<String, Object> checkInRegistration(Long registrationId) {
        requestContext.requireAdmin();
        requireId(registrationId, "registrationId");
        gatewayClient.post("/activity/admin/checkIn/" + registrationId, Map.of(), requestContext.requireGatewayToken());
        return Map.of("checkedIn", true, "registrationId", registrationId);
    }

    @Tool(description = "Confirm volunteer hours for a specific registration as an administrator. Use this after an activity is complete and the participant's hours should be officially counted. registrationId is required. Returns confirmed=true and the registrationId. Requires an admin MCP session.")
    public Map<String, Object> confirmHours(Long registrationId) {
        requestContext.requireAdmin();
        requireId(registrationId, "registrationId");
        gatewayClient.post("/activity/confirmHours/" + registrationId, Map.of(), requestContext.requireGatewayToken());
        return Map.of("confirmed", true, "registrationId", registrationId);
    }

    @Tool(description = "Confirm volunteer hours for multiple registrations in one call as an administrator. Use this for batch hour-confirmation workflows after an activity ends. Provide registrationIdsCsv as a comma-separated list such as '12,15,18'. The tool attempts each registration independently and returns counts plus per-registration success or failure details, which makes it suitable for partial-success batch operations. Requires an admin MCP session.")
    public Map<String, Object> batchConfirmHours(String registrationIdsCsv) {
        requestContext.requireAdmin();
        requireText(registrationIdsCsv, "registrationIdsCsv");

        List<Long> registrationIds = parseIdCsv(registrationIdsCsv, "registrationIdsCsv");
        List<Map<String, Object>> successes = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (Long registrationId : registrationIds) {
            try {
                gatewayClient.post("/activity/confirmHours/" + registrationId, Map.of(), requestContext.requireGatewayToken());
                successes.add(Map.of("registrationId", registrationId, "confirmed", true));
            } catch (Exception ex) {
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("registrationId", registrationId);
                failure.put("confirmed", false);
                failure.put("error", ex.getMessage());
                failures.add(failure);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedCount", registrationIds.size());
        result.put("successCount", successes.size());
        result.put("failureCount", failures.size());
        result.put("successes", successes);
        result.put("failures", failures);
        return result;
    }

    @Tool(description = "Export the currently logged-in user's confirmed volunteer record as an Excel file. Use this when the user asks to download or export their confirmed volunteer footprint or hours report. Returns fileName, contentType, byteLength, and base64Content for the generated Excel file. Requires a logged-in MCP session.")
    public Map<String, Object> exportMyConfirmedRegistrations() {
        GatewayBinaryResponse response = gatewayClient.getBinary(
                "/activity/myRegistrations/exportConfirmed",
                null,
                requestContext.requireGatewayToken()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", response.fileName());
        result.put("contentType", response.contentType());
        result.put("byteLength", response.content().length);
        result.put("base64Content", Base64.getEncoder().encodeToString(response.content()));
        return result;
    }

    private Map<String, Object> toActivitySummary(JsonNode item) {
        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("id", item.path("id").asLong());
        activity.put("title", item.path("title").asText(null));
        activity.put("category", item.path("category").asText(null));
        activity.put("status", item.path("status").asText(null));
        activity.put("location", item.path("location").asText(null));
        activity.put("volunteerHours", decimalOrNull(item, "volunteerHours"));
        activity.put("startTime", textOrNull(item, "startTime"));
        activity.put("endTime", textOrNull(item, "endTime"));
        activity.put("registrationStartTime", textOrNull(item, "registrationStartTime"));
        activity.put("registrationDeadline", textOrNull(item, "registrationDeadline"));
        activity.put("currentParticipants", intOrNull(item, "currentParticipants"));
        activity.put("maxParticipants", intOrNull(item, "maxParticipants"));
        activity.put("availableSlots", intOrNull(item, "availableSlots"));
        activity.put("isRegistered", booleanOrNull(item, "isRegistered"));
        activity.put("imageUrls", toStringList(item.path("imageUrls")));
        return activity;
    }

    private Map<String, Object> toActivityDetail(JsonNode item) {
        Map<String, Object> activity = toActivitySummary(item);
        activity.put("description", item.path("description").asText(null));
        activity.put("imageKeys", toStringList(item.path("imageKeys")));
        activity.put("imageKey", textOrNull(item, "imageKey"));
        activity.put("imageUrl", textOrNull(item, "imageUrl"));
        return activity;
    }

    private Map<String, Object> toRegistration(JsonNode item) {
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("id", item.path("id").asLong());
        registration.put("activityId", item.path("activityId").asLong());
        registration.put("activityTitle", item.path("activityTitle").asText(null));
        registration.put("location", item.path("location").asText(null));
        registration.put("volunteerHours", decimalOrNull(item, "volunteerHours"));
        registration.put("startTime", textOrNull(item, "startTime"));
        registration.put("registrationTime", textOrNull(item, "registrationTime"));
        registration.put("checkInStatus", intOrNull(item, "checkInStatus"));
        registration.put("checkInTime", textOrNull(item, "checkInTime"));
        registration.put("hoursConfirmed", intOrNull(item, "hoursConfirmed"));
        registration.put("confirmTime", textOrNull(item, "confirmTime"));
        registration.put("status", item.path("status").asText(null));
        return registration;
    }

    private Map<String, Object> toAdminRegistration(JsonNode item) {
        Map<String, Object> registration = toRegistration(item);
        registration.put("userId", item.hasNonNull("userId") ? item.path("userId").asLong() : null);
        registration.put("username", textOrNull(item, "username"));
        registration.put("realName", textOrNull(item, "realName"));
        registration.put("studentNo", textOrNull(item, "studentNo"));
        registration.put("phone", textOrNull(item, "phone"));
        return registration;
    }

    private void addIfHasText(MultiValueMap<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key, value.trim());
        }
    }

    private List<Map<String, Object>> toActivityList(JsonNode data) {
        List<Map<String, Object>> activities = new ArrayList<>();
        for (JsonNode item : data) {
            activities.add(toActivitySummary(item));
        }
        return activities;
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

    private List<Long> parseIdCsv(String csv, String fieldName) {
        List<String> rawValues = splitCsv(csv);
        if (rawValues.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must contain at least one id");
        }
        List<Long> ids = new ArrayList<>();
        for (String rawValue : rawValues) {
            try {
                long id = Long.parseLong(rawValue);
                if (id < 1) {
                    throw new NumberFormatException("id must be positive");
                }
                ids.add(id);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(fieldName + " contains an invalid id: " + rawValue, ex);
            }
        }
        return ids;
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

    private Integer intOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asInt() : null;
    }

    private Boolean booleanOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asBoolean() : null;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asText() : null;
    }

    private BigDecimal decimalOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).decimalValue() : null;
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
        String actualFilename = filename == null || filename.isBlank() ? "activity-image" : filename.trim();
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
