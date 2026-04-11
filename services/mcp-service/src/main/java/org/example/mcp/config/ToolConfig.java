package org.example.mcp.config;

import org.example.mcp.tool.ActivityMcpTools;
import org.example.mcp.tool.UserMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ActivityMcpTools activityMcpTools, UserMcpTools userMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(activityMcpTools, userMcpTools)
                .build();
    }
}
