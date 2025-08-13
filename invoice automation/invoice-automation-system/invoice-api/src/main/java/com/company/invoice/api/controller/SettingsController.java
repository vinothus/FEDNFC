package com.company.invoice.api.controller;

import com.company.invoice.api.dto.request.SettingUpdateRequest;
import com.company.invoice.api.dto.response.SettingResponse;
import com.company.invoice.api.service.SettingsService;
import com.company.invoice.data.entity.SystemSetting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for system settings management
 */
@RestController
@RequestMapping("/admin/settings")
@Tag(name = "Settings Management", description = "Admin endpoints for managing system settings")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get all settings", description = "Get all system settings (admin view with sensitive data)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettingResponse>> getAllSettings() {
        try {
            log.info("🔧 Admin: Getting all settings");
            
            List<SettingResponse> settings = settingsService.getAllSettingsAdmin();
            
            log.info("✅ Admin: Retrieved {} settings", settings.size());
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting settings: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get settings by category", description = "Get settings filtered by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSettingsByCategory(
            @Parameter(description = "Setting category") @PathVariable String category) {
        try {
            log.info("📂 Admin: Getting settings by category: {}", category);
            
            // Validate category
            SystemSetting.SettingCategory settingCategory;
            try {
                settingCategory = SystemSetting.SettingCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid category", "message", "Valid categories: " + 
                        String.join(", ", getValidCategories())));
            }
            
            List<SettingResponse> settings = settingsService.getSettingsByCategory(settingCategory, true);
            
            log.info("✅ Admin: Retrieved {} settings for category: {}", settings.size(), category);
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting settings by category: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get setting by key", description = "Get specific setting by key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Setting retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Setting not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettingResponse> getSettingByKey(
            @Parameter(description = "Setting key") @PathVariable String key) {
        try {
            log.info("🔍 Admin: Getting setting by key: {}", key);
            
            return settingsService.getSettingByKey(key, true)
                .map(setting -> {
                    log.info("✅ Admin: Retrieved setting: {}", setting.getDisplayName());
                    return ResponseEntity.ok(setting);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting setting by key: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update setting", description = "Update system setting value")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Setting updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid setting value"),
        @ApiResponse(responseCode = "404", description = "Setting not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSetting(
            @Parameter(description = "Setting key") @PathVariable String key,
            @Valid @RequestBody SettingUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("✏️ Admin: Updating setting: {} by user: {}", key, userDetails.getUsername());
            
            return settingsService.updateSetting(key, request, userDetails.getUsername())
                .map(setting -> {
                    log.info("✅ Admin: Setting updated successfully: {}", setting.getDisplayName());
                    return ResponseEntity.ok(setting);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (IllegalArgumentException e) {
            log.warn("❌ Admin: Invalid setting value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid setting value", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error updating setting: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update setting", "message", e.getMessage()));
        }
    }

    @PostMapping("/{key}/reset")
    @Operation(summary = "Reset setting to default", description = "Reset setting value to its default")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Setting reset successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot reset read-only setting"),
        @ApiResponse(responseCode = "404", description = "Setting not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetSettingToDefault(
            @Parameter(description = "Setting key") @PathVariable String key,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("🔄 Admin: Resetting setting to default: {} by user: {}", key, userDetails.getUsername());
            
            return settingsService.resetSettingToDefault(key, userDetails.getUsername())
                .map(setting -> {
                    log.info("✅ Admin: Setting reset to default: {}", setting.getDisplayName());
                    return ResponseEntity.ok(setting);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (IllegalArgumentException e) {
            log.warn("❌ Admin: Cannot reset setting: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Cannot reset setting", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error resetting setting: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to reset setting", "message", e.getMessage()));
        }
    }

    @GetMapping("/modified")
    @Operation(summary = "Get modified settings", description = "Get settings that have been changed from default")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modified settings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettingResponse>> getModifiedSettings() {
        try {
            log.info("📋 Admin: Getting modified settings");
            
            List<SettingResponse> settings = settingsService.getModifiedSettings(true);
            
            log.info("✅ Admin: Retrieved {} modified settings", settings.size());
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting modified settings: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search settings", description = "Search settings by key, name, or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search query"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchSettings(
            @Parameter(description = "Search query") @RequestParam String query) {
        try {
            log.info("🔍 Admin: Searching settings with query: {}", query);
            
            if (query == null || query.trim().length() < 2) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid search query", "message", "Query must be at least 2 characters"));
            }
            
            List<SettingResponse> settings = settingsService.searchSettings(query.trim(), true);
            
            log.info("✅ Admin: Found {} settings matching query: {}", settings.size(), query);
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error searching settings: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/restart-required")
    @Operation(summary = "Get settings requiring restart", description = "Get modified settings that require application restart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings requiring restart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettingResponse>> getSettingsRequiringRestart() {
        try {
            log.info("🔄 Admin: Getting settings requiring restart");
            
            List<SettingResponse> settings = settingsService.getSettingsRequiringRestart(true);
            
            log.info("✅ Admin: Retrieved {} settings requiring restart", settings.size());
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting settings requiring restart: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get settings statistics", description = "Get statistics about system settings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSettingsStatistics() {
        try {
            log.info("📊 Admin: Getting settings statistics");
            
            Map<String, Object> statistics = settingsService.getSettingsStatistics();
            
            log.info("✅ Admin: Generated settings statistics");
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting settings statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "Get available categories", description = "Get list of available setting categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAvailableCategories() {
        try {
            log.info("📋 Admin: Getting available setting categories");
            
            List<String> categories = getValidCategories();
            
            return ResponseEntity.ok(categories);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting categories: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get valid setting categories
     */
    private List<String> getValidCategories() {
        return List.of(
            SystemSetting.SettingCategory.GENERAL.name(),
            SystemSetting.SettingCategory.EMAIL.name(),
            SystemSetting.SettingCategory.OCR.name(),
            SystemSetting.SettingCategory.SECURITY.name(),
            SystemSetting.SettingCategory.NOTIFICATIONS.name(),
            SystemSetting.SettingCategory.PERFORMANCE.name(),
            SystemSetting.SettingCategory.INTEGRATION.name(),
            SystemSetting.SettingCategory.LOGGING.name()
        );
    }
}
