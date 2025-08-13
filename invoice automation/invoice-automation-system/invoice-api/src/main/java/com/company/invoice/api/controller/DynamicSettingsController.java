package com.company.invoice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing dynamic application settings
 * Allows admins to view and modify runtime configuration
 */
@RestController
@RequestMapping("/admin/dynamic-settings")
@Tag(name = "Dynamic Settings", description = "Admin endpoints for managing dynamic application settings")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class DynamicSettingsController {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    // Define which settings can be modified at runtime
    private static final Map<String, SettingDefinition> DYNAMIC_SETTINGS;
    
    static {
        Map<String, SettingDefinition> settings = new HashMap<>();
        
        // File Storage Settings
        settings.put("invoice.storage.max-file-size", new SettingDefinition("File Storage", "Maximum file size for uploads", "50MB", "string", false));
        settings.put("invoice.storage.pdf-directory", new SettingDefinition("File Storage", "PDF storage directory path", "./pdf-storage", "string", true));
        
        // OCR Settings
        settings.put("invoice.ocr.tesseract.language", new SettingDefinition("OCR", "Tesseract OCR language", "eng", "string", false));
        settings.put("invoice.ocr.tesseract.dpi", new SettingDefinition("OCR", "OCR processing DPI", "300", "number", false));
        settings.put("invoice.ocr.tesseract.timeout-seconds", new SettingDefinition("OCR", "OCR processing timeout (seconds)", "120", "number", false));
        settings.put("invoice.ocr.coordinator.min-confidence", new SettingDefinition("OCR", "Minimum confidence threshold", "0.7", "number", false));
        settings.put("invoice.ocr.coordinator.enable-fallback", new SettingDefinition("OCR", "Enable OCR fallback methods", "true", "boolean", false));
        settings.put("invoice.ocr.coordinator.parallel-processing", new SettingDefinition("OCR", "Enable parallel OCR processing", "true", "boolean", false));
        settings.put("invoice.ocr.coordinator.timeout-minutes", new SettingDefinition("OCR", "OCR coordinator timeout (minutes)", "5", "number", false));
        
        // Email Settings
        settings.put("invoice.email.monitoring.enabled", new SettingDefinition("Email", "Enable email monitoring", "true", "boolean", false));
        settings.put("invoice.email.monitoring.poll-interval", new SettingDefinition("Email", "Email polling interval (ms)", "120000", "number", false));
        settings.put("invoice.email.monitoring.days-to-check", new SettingDefinition("Email", "Days to check for emails", "2", "number", false));
        
        // Security Settings
        settings.put("spring.security.jwt.expiration", new SettingDefinition("Security", "JWT token expiration (ms)", "86400000", "number", true));
        settings.put("invoice.security.cors.allowed-origins", new SettingDefinition("Security", "CORS allowed origins", "http://localhost:3000", "string", true));
        
        // Logging Settings
        settings.put("logging.level.com.company.invoice", new SettingDefinition("Logging", "Application log level", "INFO", "enum", false, Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR")));
        settings.put("logging.level.org.springframework.security", new SettingDefinition("Logging", "Spring Security log level", "DEBUG", "enum", false, Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR")));
        
        // Scheduler Settings
        settings.put("invoice.scheduler.enabled", new SettingDefinition("Scheduler", "Enable task scheduler", "true", "boolean", false));
        settings.put("invoice.scheduler.heartbeat-interval", new SettingDefinition("Scheduler", "Scheduler heartbeat interval (ms)", "30000", "number", false));
        
        // Management Settings
        settings.put("management.endpoint.health.show-details", new SettingDefinition("Management", "Health endpoint detail level", "when-authorized", "enum", false, Arrays.asList("never", "when-authorized", "always")));
        
        DYNAMIC_SETTINGS = Collections.unmodifiableMap(settings);
    }

    @GetMapping
    @Operation(summary = "Get all dynamic settings", description = "Retrieve all configurable application settings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        try {
            log.info("🔧 Admin: Retrieving all dynamic settings");

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            
            List<Map<String, Object>> settings = DYNAMIC_SETTINGS.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    SettingDefinition definition = entry.getValue();
                    String currentValue = environment.getProperty(key, definition.getDefaultValue());
                    
                    Map<String, Object> setting = new LinkedHashMap<>();
                    setting.put("key", key);
                    setting.put("category", definition.getCategory());
                    setting.put("name", definition.getName());
                    setting.put("description", definition.getDescription());
                    setting.put("currentValue", currentValue);
                    setting.put("defaultValue", definition.getDefaultValue());
                    setting.put("type", definition.getType());
                    setting.put("requiresRestart", definition.isRequiresRestart());
                    if (definition.getAllowedValues() != null) {
                        setting.put("allowedValues", definition.getAllowedValues());
                    }
                    setting.put("lastModified", System.currentTimeMillis());
                    setting.put("modifiedBy", "system");
                    
                    return setting;
                })
                .collect(Collectors.toList());

            // Group by category
            Map<String, List<Map<String, Object>>> groupedSettings = settings.stream()
                .collect(Collectors.groupingBy(s -> (String) s.get("category")));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("settings", settings);
            response.put("categories", groupedSettings.keySet().stream().sorted().collect(Collectors.toList()));
            response.put("groupedSettings", groupedSettings);
            response.put("totalSettings", settings.size());
            response.put("modifiableSettings", settings.stream().filter(s -> !(Boolean) s.get("requiresRestart")).count());
            response.put("restartRequired", settings.stream().anyMatch(s -> (Boolean) s.get("requiresRestart")));

            log.info("✅ Admin: Retrieved {} dynamic settings", settings.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🚨 Admin: Error retrieving dynamic settings: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve settings", "message", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "Get dynamic setting categories", description = "Get list of dynamic setting categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        try {
            List<Map<String, Object>> categories = DYNAMIC_SETTINGS.values().stream()
                .collect(Collectors.groupingBy(SettingDefinition::getCategory, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.of(
                    "name", (Object) entry.getKey(),
                    "count", entry.getValue()
                ))
                .sorted((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")))
                .collect(Collectors.toList());

            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("🚨 Admin: Error retrieving setting categories: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get specific setting", description = "Get details of a specific setting")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSetting(@PathVariable String key) {
        try {
            if (!DYNAMIC_SETTINGS.containsKey(key)) {
                return ResponseEntity.notFound().build();
            }

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            SettingDefinition definition = DYNAMIC_SETTINGS.get(key);
            String currentValue = environment.getProperty(key, definition.getDefaultValue());

            Map<String, Object> setting = new LinkedHashMap<>();
            setting.put("key", key);
            setting.put("category", definition.getCategory());
            setting.put("name", definition.getName());
            setting.put("description", definition.getDescription());
            setting.put("currentValue", currentValue);
            setting.put("defaultValue", definition.getDefaultValue());
            setting.put("type", definition.getType());
            setting.put("requiresRestart", definition.isRequiresRestart());
            if (definition.getAllowedValues() != null) {
                setting.put("allowedValues", definition.getAllowedValues());
            }

            return ResponseEntity.ok(setting);
        } catch (Exception e) {
            log.error("🚨 Admin: Error retrieving setting {}: {}", key, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update setting", description = "Update a specific setting value")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateSetting(
            @PathVariable String key, 
            @RequestBody @Valid Map<String, String> request) {
        try {
            if (!DYNAMIC_SETTINGS.containsKey(key)) {
                return ResponseEntity.notFound().build();
            }

            String newValue = request.get("value");
            if (newValue == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Value is required"));
            }

            SettingDefinition definition = DYNAMIC_SETTINGS.get(key);
            
            // Validate the new value
            String validationError = validateSettingValue(definition, newValue);
            if (validationError != null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", validationError));
            }

            log.info("🔧 Admin: Updating setting {} from '{}' to '{}'", key, 
                applicationContext.getEnvironment().getProperty(key), newValue);

            // Update the property in the environment
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();
            
            // Create or update the dynamic properties source
            MapPropertySource dynamicSource = (MapPropertySource) propertySources.get("dynamicSettings");
            if (dynamicSource == null) {
                Map<String, Object> dynamicProps = new HashMap<>();
                dynamicProps.put(key, newValue);
                dynamicSource = new MapPropertySource("dynamicSettings", dynamicProps);
                propertySources.addFirst(dynamicSource);
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) dynamicSource.getSource();
                source.put(key, newValue);
            }

            // Return the updated setting
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("key", key);
            response.put("previousValue", environment.getProperty(key, definition.getDefaultValue()));
            response.put("newValue", newValue);
            response.put("requiresRestart", definition.isRequiresRestart());
            response.put("success", true);
            response.put("message", "Setting updated successfully");

            log.info("✅ Admin: Setting {} updated successfully", key);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🚨 Admin: Error updating setting {}: {}", key, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update setting", "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk-update")
    @Operation(summary = "Bulk update settings", description = "Update multiple settings at once")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkUpdateSettings(@RequestBody Map<String, String> settings) {
        try {
            log.info("🔧 Admin: Bulk updating {} settings", settings.size());

            List<Map<String, Object>> results = new ArrayList<>();
            boolean hasErrors = false;

            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("key", key);

                if (!DYNAMIC_SETTINGS.containsKey(key)) {
                    result.put("success", false);
                    result.put("error", "Setting not found");
                    hasErrors = true;
                } else {
                    SettingDefinition definition = DYNAMIC_SETTINGS.get(key);
                    String validationError = validateSettingValue(definition, value);
                    
                    if (validationError != null) {
                        result.put("success", false);
                        result.put("error", validationError);
                        hasErrors = true;
                    } else {
                        // Update the setting
                        ConfigurableEnvironment environment = applicationContext.getEnvironment();
                        MutablePropertySources propertySources = environment.getPropertySources();
                        
                        MapPropertySource dynamicSource = (MapPropertySource) propertySources.get("dynamicSettings");
                        if (dynamicSource == null) {
                            Map<String, Object> dynamicProps = new HashMap<>();
                            dynamicProps.put(key, value);
                            dynamicSource = new MapPropertySource("dynamicSettings", dynamicProps);
                            propertySources.addFirst(dynamicSource);
                        } else {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> source = (Map<String, Object>) dynamicSource.getSource();
                            source.put(key, value);
                        }

                        result.put("success", true);
                        result.put("newValue", value);
                        result.put("requiresRestart", definition.isRequiresRestart());
                    }
                }

                results.add(result);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("results", results);
            response.put("totalUpdated", results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum());
            response.put("totalErrors", results.stream().mapToLong(r -> (Boolean) r.get("success") ? 0 : 1).sum());
            response.put("hasErrors", hasErrors);
            response.put("restartRequired", results.stream().anyMatch(r -> 
                (Boolean) r.getOrDefault("success", false) && (Boolean) r.getOrDefault("requiresRestart", false)));

            log.info("✅ Admin: Bulk update completed - {} successful, {} errors", 
                response.get("totalUpdated"), response.get("totalErrors"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🚨 Admin: Error during bulk settings update: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update settings", "message", e.getMessage()));
        }
    }

    @PostMapping("/reset/{key}")
    @Operation(summary = "Reset setting to default", description = "Reset a setting to its default value")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetSetting(@PathVariable String key) {
        try {
            if (!DYNAMIC_SETTINGS.containsKey(key)) {
                return ResponseEntity.notFound().build();
            }

            SettingDefinition definition = DYNAMIC_SETTINGS.get(key);
            String defaultValue = definition.getDefaultValue();

            log.info("🔧 Admin: Resetting setting {} to default value '{}'", key, defaultValue);

            // Update to default value
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();
            
            MapPropertySource dynamicSource = (MapPropertySource) propertySources.get("dynamicSettings");
            if (dynamicSource != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) dynamicSource.getSource();
                source.remove(key); // Remove to fall back to default
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("key", key);
            response.put("defaultValue", defaultValue);
            response.put("requiresRestart", definition.isRequiresRestart());
            response.put("success", true);
            response.put("message", "Setting reset to default");

            log.info("✅ Admin: Setting {} reset to default", key);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🚨 Admin: Error resetting setting {}: {}", key, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to reset setting", "message", e.getMessage()));
        }
    }

    private String validateSettingValue(SettingDefinition definition, String value) {
        try {
            switch (definition.getType()) {
                case "boolean":
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        return "Value must be 'true' or 'false'";
                    }
                    break;
                case "number":
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        return "Value must be a valid number";
                    }
                    break;
                case "enum":
                    if (definition.getAllowedValues() != null && 
                        !definition.getAllowedValues().contains(value)) {
                        return "Value must be one of: " + definition.getAllowedValues();
                    }
                    break;
                case "string":
                    if (value.trim().isEmpty()) {
                        return "Value cannot be empty";
                    }
                    break;
            }
            return null;
        } catch (Exception e) {
            return "Invalid value format";
        }
    }

    // Helper class for setting definitions
    private static class SettingDefinition {
        private final String category;
        private final String name;
        private final String description;
        private final String defaultValue;
        private final String type;
        private final boolean requiresRestart;
        private final List<String> allowedValues;

        public SettingDefinition(String category, String description, String defaultValue, String type, boolean requiresRestart) {
            this(category, description, defaultValue, type, requiresRestart, null);
        }

        public SettingDefinition(String category, String description, String defaultValue, String type, boolean requiresRestart, List<String> allowedValues) {
            this.category = category;
            this.name = description;
            this.description = description;
            this.defaultValue = defaultValue;
            this.type = type;
            this.requiresRestart = requiresRestart;
            this.allowedValues = allowedValues;
        }

        public String getCategory() { return category; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getDefaultValue() { return defaultValue; }
        public String getType() { return type; }
        public boolean isRequiresRestart() { return requiresRestart; }
        public List<String> getAllowedValues() { return allowedValues; }
    }
}
