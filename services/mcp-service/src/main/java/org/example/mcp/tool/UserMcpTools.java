package org.example.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.mcp.auth.McpJwtClaims;
import org.example.mcp.auth.McpRequestContext;
import org.example.mcp.gateway.CloudDemoGatewayClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserMcpTools {

    private final CloudDemoGatewayClient gatewayClient;
    private final McpRequestContext requestContext;

    public UserMcpTools(CloudDemoGatewayClient gatewayClient, McpRequestContext requestContext) {
        this.gatewayClient = gatewayClient;
        this.requestContext = requestContext;
    }

    @Tool(description = "Get the full profile of the currently logged-in user. Use this when the user asks to view their account information, personal details, role, or accumulated volunteer hours. Returns fields such as id, username, realName, studentNo, phone, email, role, and totalVolunteerHours. Requires a logged-in MCP session.")
    public Map<String, Object> getMyProfile() {
        JsonNode data = gatewayClient.get("/user/info", null, requestContext.requireGatewayToken());
        return toUserProfile(data);
    }

    @Tool(description = "Update the profile of the currently logged-in user. Use this when the user wants to modify personal details such as real name, student number, phone, or email. Provide only the fields that should change and leave unrelated fields null or empty. Returns a small confirmation payload with updated=true, userId, and username. Requires a logged-in MCP session.")
    public Map<String, Object> updateMyProfile(
            String realName,
            String studentNo,
            String phone,
            String email
    ) {
        McpJwtClaims claims = requestContext.requireClaims();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("id", claims.userId());
        request.put("username", claims.username());
        request.put("realName", realName);
        request.put("studentNo", studentNo);
        request.put("phone", phone);
        request.put("email", email);
        request.put("role", claims.role());

        gatewayClient.put("/user/update", request, requestContext.requireGatewayToken());
        return Map.of(
                "updated", true,
                "userId", claims.userId(),
                "username", claims.username()
        );
    }

    @Tool(description = "Change the password of the currently logged-in user. Use this only when the user explicitly asks to update their password. Both oldPassword and newPassword are required plain-text inputs. Returns updated=true if the password change succeeds. Requires a logged-in MCP session.")
    public Map<String, Object> updateMyPassword(String oldPassword, String newPassword) {
        requireText(oldPassword, "oldPassword");
        requireText(newPassword, "newPassword");

        gatewayClient.put("/user/updatePassword", Map.of(
                "oldPassword", oldPassword,
                "newPassword", newPassword
        ), requestContext.requireGatewayToken());

        return Map.of("updated", true);
    }

    @Tool(description = "List volunteer hour summaries for users, intended for administrators. Use this when an admin wants to search volunteer-hour totals across users. The optional keyword can match username, realName, studentNo, phone, or similar searchable identity fields supported by the backend. Returns a list of user profiles including totalVolunteerHours. Requires an admin MCP session.")
    public List<Map<String, Object>> listVolunteerHours(String keyword) {
        requestContext.requireAdmin();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        addIfHasText(params, "keyword", keyword);

        JsonNode data = gatewayClient.get("/user/admin/hours", params, requestContext.requireGatewayToken());
        List<Map<String, Object>> users = new ArrayList<>();
        for (JsonNode item : data) {
            users.add(toUserProfile(item));
        }
        return users;
    }

    @Tool(description = "Register a new volunteer account through the public user-registration API. Use this when a new user explicitly wants to create an account for the platform. Provide username and password, and optionally realName, studentNo, phone, and email. Returns registered=true plus a summary of the submitted identity fields. Does not require an existing MCP login session.")
    public Map<String, Object> registerUser(
            String username,
            String password,
            String realName,
            String studentNo,
            String phone,
            String email
    ) {
        requireText(username, "username");
        requireText(password, "password");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("realName", realName);
        request.put("studentNo", studentNo);
        request.put("phone", phone);
        request.put("email", email);

        gatewayClient.post("/user/register", request, null);
        return Map.of(
                "registered", true,
                "username", username,
                "realName", realName,
                "studentNo", studentNo
        );
    }

    @Tool(description = "Build a concise volunteer summary for the currently logged-in user. Use this when the user asks for an overview of their volunteer status, such as total hours, how many activities they joined, how many are upcoming, how many still need check-in or hour confirmation, and what the next upcoming activity is. Returns profile fields together with aggregate registration statistics and an optional nextUpcomingActivity object. Requires a logged-in MCP session.")
    public Map<String, Object> getMyVolunteerSummary() {
        Map<String, Object> profile = getMyProfile();
        JsonNode data = gatewayClient.get("/activity/myRegistrations", null, requestContext.requireGatewayToken());

        List<Map<String, Object>> registrations = new ArrayList<>();
        BigDecimal confirmedHours = BigDecimal.ZERO;
        int activeRegistrations = 0;
        int confirmedCount = 0;
        int checkedInCount = 0;
        int pendingCheckInCount = 0;
        int pendingHourConfirmationCount = 0;
        Map<String, Object> nextUpcomingActivity = null;
        long nextUpcomingStartEpoch = Long.MAX_VALUE;

        for (JsonNode item : data) {
            Map<String, Object> registration = toRegistration(item);
            registrations.add(registration);

            String status = textOrNull(item, "status");
            Integer checkInStatus = intOrNull(item, "checkInStatus");
            Integer hoursConfirmed = intOrNull(item, "hoursConfirmed");

            if ("REGISTERED".equals(status)) {
                activeRegistrations++;
                if (checkInStatus != null && checkInStatus == 1) {
                    checkedInCount++;
                } else {
                    pendingCheckInCount++;
                }
                if (hoursConfirmed != null && hoursConfirmed == 1) {
                    confirmedCount++;
                    BigDecimal hours = decimalOrNull(item, "volunteerHours");
                    if (hours != null) {
                        confirmedHours = confirmedHours.add(hours);
                    }
                } else {
                    pendingHourConfirmationCount++;
                }
            }

            String startTime = textOrNull(item, "startTime");
            if ("REGISTERED".equals(status) && startTime != null) {
                long startEpoch = parseSortableEpoch(startTime);
                if (startEpoch > System.currentTimeMillis() && startEpoch < nextUpcomingStartEpoch) {
                    nextUpcomingStartEpoch = startEpoch;
                    nextUpcomingActivity = new LinkedHashMap<>();
                    nextUpcomingActivity.put("activityId", registration.get("activityId"));
                    nextUpcomingActivity.put("activityTitle", registration.get("activityTitle"));
                    nextUpcomingActivity.put("location", registration.get("location"));
                    nextUpcomingActivity.put("startTime", startTime);
                    nextUpcomingActivity.put("volunteerHours", registration.get("volunteerHours"));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.put("activeRegistrationCount", activeRegistrations);
        result.put("confirmedRegistrationCount", confirmedCount);
        result.put("checkedInRegistrationCount", checkedInCount);
        result.put("pendingCheckInCount", pendingCheckInCount);
        result.put("pendingHourConfirmationCount", pendingHourConfirmationCount);
        result.put("confirmedVolunteerHoursFromRegistrations", confirmedHours);
        result.put("nextUpcomingActivity", nextUpcomingActivity);
        result.put("recentRegistrations", registrations.stream()
                .sorted(Comparator.comparing(
                        item -> String.valueOf(item.getOrDefault("startTime", "")),
                        Comparator.reverseOrder()))
                .limit(5)
                .toList());
        return result;
    }

    private Map<String, Object> toUserProfile(JsonNode item) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", item.hasNonNull("id") ? item.path("id").asLong() : null);
        profile.put("username", textOrNull(item, "username"));
        profile.put("realName", textOrNull(item, "realName"));
        profile.put("studentNo", textOrNull(item, "studentNo"));
        profile.put("phone", textOrNull(item, "phone"));
        profile.put("email", textOrNull(item, "email"));
        profile.put("role", textOrNull(item, "role"));
        profile.put("totalVolunteerHours", item.hasNonNull("totalVolunteerHours") ? item.path("totalVolunteerHours").decimalValue() : null);
        return profile;
    }

    private void addIfHasText(MultiValueMap<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key, value.trim());
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private Map<String, Object> toRegistration(JsonNode item) {
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("id", item.path("id").asLong());
        registration.put("activityId", item.path("activityId").asLong());
        registration.put("activityTitle", item.path("activityTitle").asText(null));
        registration.put("location", textOrNull(item, "location"));
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

    private Integer intOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asInt() : null;
    }

    private BigDecimal decimalOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).decimalValue() : null;
    }

    private long parseSortableEpoch(String value) {
        try {
            return java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return java.time.LocalDateTime.parse(value)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
        }
        return Long.MAX_VALUE;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.path(fieldName).asText() : null;
    }
}
