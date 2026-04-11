package org.example.mcp.gateway;

public record GatewayBinaryResponse(
        byte[] content,
        String contentType,
        String fileName
) {
}
