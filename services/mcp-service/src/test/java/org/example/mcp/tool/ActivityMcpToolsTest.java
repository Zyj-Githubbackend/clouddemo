package org.example.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mcp.auth.McpRequestContext;
import org.example.mcp.gateway.CloudDemoGatewayClient;
import org.example.mcp.gateway.GatewayBinaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityMcpToolsTest {

    @Mock
    private CloudDemoGatewayClient gatewayClient;

    @Mock
    private McpRequestContext requestContext;

    private ActivityMcpTools tools;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tools = new ActivityMcpTools(gatewayClient, requestContext);
    }

    @Test
    void listActivitiesShouldNormalizePagingAndMapGatewayResponse() throws Exception {
        when(requestContext.requireGatewayToken()).thenReturn("Bearer token");
        JsonNode data = objectMapper.readTree("""
                {
                  "current": 1,
                  "size": 10,
                  "total": 1,
                  "pages": 1,
                  "records": [
                    {
                      "id": 9,
                      "title": "图书整理",
                      "category": "校园服务",
                      "status": "RECRUITING",
                      "location": "图书馆",
                      "volunteerHours": 1.5,
                      "availableSlots": 12,
                      "isRegistered": true,
                      "imageUrls": ["http://img/1.png"]
                    }
                  ]
                }
                """);
        when(gatewayClient.get(eq("/activity/list"), any(), eq("Bearer token"))).thenReturn(data);

        Map<String, Object> result = tools.listActivities(0, 100, " RECRUITING ", " 校园服务 ", "OPEN");

        assertEquals(1L, result.get("total"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activities = (List<Map<String, Object>>) result.get("activities");
        assertEquals(1, activities.size());
        assertEquals(9L, activities.get(0).get("id"));
        assertEquals(new BigDecimal("1.5"), activities.get(0).get("volunteerHours"));
        assertEquals(List.of("http://img/1.png"), activities.get(0).get("imageUrls"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<MultiValueMap<String, String>> paramsCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(gatewayClient).get(eq("/activity/list"), paramsCaptor.capture(), eq("Bearer token"));
        MultiValueMap<String, String> params = paramsCaptor.getValue();
        assertEquals("1", params.getFirst("page"));
        assertEquals("50", params.getFirst("size"));
        assertEquals("RECRUITING", params.getFirst("status"));
        assertEquals("校园服务", params.getFirst("category"));
    }

    @Test
    void createActivityShouldRequireAdminAndSplitImageKeys() {
        when(requestContext.requireGatewayToken()).thenReturn("Bearer admin-token");

        Map<String, Object> result = tools.createActivity(
                "活动",
                "说明",
                "操场",
                30,
                new BigDecimal("2.0"),
                "2026-05-01T09:00:00",
                "2026-05-01T11:00:00",
                "2026-04-25T09:00:00",
                "2026-04-30T18:00:00",
                "校园服务",
                "legacy.png",
                " a.png, ,b.png "
        );

        assertEquals(true, result.get("created"));
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestContext).requireAdmin();
        verify(gatewayClient).post(eq("/activity/create"), bodyCaptor.capture(), eq("Bearer admin-token"));
        assertEquals(List.of("a.png", "b.png"), bodyCaptor.getValue().get("imageKeys"));
    }

    @Test
    void createActivityShouldNotCallGatewayWhenAdminCheckFails() {
        doThrow(new IllegalStateException("Admin role is required")).when(requestContext).requireAdmin();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tools.createActivity(
                        "活动",
                        "说明",
                        "操场",
                        30,
                        new BigDecimal("2.0"),
                        "2026-05-01T09:00:00",
                        "2026-05-01T11:00:00",
                        "2026-04-25T09:00:00",
                        "2026-04-30T18:00:00",
                        "校园服务",
                        null,
                        null
                ));

        assertEquals("Admin role is required", ex.getMessage());
        verify(gatewayClient, never()).post(any(), any(), any());
    }

    @Test
    void uploadActivityImageShouldParseDataUrlAndAppendExtension() throws Exception {
        when(requestContext.requireGatewayToken()).thenReturn("Bearer admin-token");
        JsonNode response = objectMapper.readTree("""
                {
                  "imageKey": "activity/1.png",
                  "imageUrl": "http://img/activity/1.png"
                }
                """);
        when(gatewayClient.postMultipart(
                eq("/activity/admin/image"),
                eq("file"),
                any(byte[].class),
                eq("poster.png"),
                eq("image/png"),
                eq("Bearer admin-token")
        )).thenReturn(response);

        Map<String, Object> result = tools.uploadActivityImage(
                "poster",
                "",
                "data:image/png;base64,aGVsbG8="
        );

        assertEquals("activity/1.png", result.get("imageKey"));
        assertEquals("poster.png", result.get("filename"));
        assertEquals("image/png", result.get("contentType"));
        assertEquals(5, result.get("byteLength"));

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(requestContext).requireAdmin();
        verify(gatewayClient).postMultipart(
                eq("/activity/admin/image"),
                eq("file"),
                contentCaptor.capture(),
                eq("poster.png"),
                eq("image/png"),
                eq("Bearer admin-token")
        );
        assertArrayEquals("hello".getBytes(), contentCaptor.getValue());
    }

    @Test
    void exportMyConfirmedRegistrationsShouldReturnBase64FilePayload() {
        when(requestContext.requireGatewayToken()).thenReturn("Bearer token");
        when(gatewayClient.getBinary("/activity/myRegistrations/exportConfirmed", null, "Bearer token"))
                .thenReturn(new GatewayBinaryResponse("xlsx".getBytes(), "application/vnd.ms-excel", "records.xlsx"));

        Map<String, Object> result = tools.exportMyConfirmedRegistrations();

        assertEquals("records.xlsx", result.get("fileName"));
        assertEquals("application/vnd.ms-excel", result.get("contentType"));
        assertEquals(4, result.get("byteLength"));
        assertEquals("eGxzeA==", result.get("base64Content"));
    }
}
