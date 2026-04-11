package org.example.mcp.config;

import org.example.mcp.auth.McpAccessTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<McpAccessTokenFilter> mcpAccessTokenFilterRegistration(McpAccessTokenFilter filter) {
        FilterRegistrationBean<McpAccessTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
