package com.company.invoice.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${invoice.security.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${invoice.security.cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${invoice.security.cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${invoice.security.cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/invoice-automation/**", configuration);
        source.registerCorsConfiguration("/**", configuration); // Allow CORS for frontend routes
        return source;
    }
    
    /**
     * Configure resource handlers for serving static files
     * Ensures React app static files are served correctly
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve React static files (JS, CSS) from build output
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/")
                .setCachePeriod(86400); // Cache for 1 day
                
        // Serve React public assets (favicon, logos, manifest, etc.)
        registry.addResourceHandler("/favicon.ico", "/manifest.json", "/robots.txt", "/logo192.png", "/logo512.png")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // Cache for 1 day
                
                        // Fallback handler for other potential static resources
        registry.addResourceHandler("/*.png", "/*.jpg", "/*.ico", "/*.json", "/*.txt")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // Cache for 1 day
    }
}
