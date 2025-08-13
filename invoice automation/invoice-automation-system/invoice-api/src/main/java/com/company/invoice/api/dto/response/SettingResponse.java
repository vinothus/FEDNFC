package com.company.invoice.api.dto.response;

import com.company.invoice.data.entity.SystemSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for system settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System setting response")
public class SettingResponse {

    @Schema(description = "Setting ID", example = "1")
    private Long id;

    @Schema(description = "Setting key", example = "email.smtp.host")
    private String settingKey;

    @Schema(description = "Current setting value", example = "smtp.example.com")
    private String settingValue;

    @Schema(description = "Setting data type", example = "STRING")
    private String settingType;

    @Schema(description = "Setting category", example = "EMAIL")
    private String category;

    @Schema(description = "Display name", example = "SMTP Host")
    private String displayName;

    @Schema(description = "Setting description", example = "SMTP server hostname for outgoing emails")
    private String description;

    @Schema(description = "Default value", example = "localhost")
    private String defaultValue;

    @Schema(description = "Validation regex pattern")
    private String validationRegex;

    @Schema(description = "Minimum value for numeric settings")
    private Double minValue;

    @Schema(description = "Maximum value for numeric settings")
    private Double maxValue;

    @Schema(description = "Allowed values for enum settings")
    private List<String> allowedValues;

    @Schema(description = "Whether setting contains sensitive data", example = "false")
    private Boolean isSensitive;

    @Schema(description = "Whether setting is read-only", example = "false")
    private Boolean isReadonly;

    @Schema(description = "Whether changing this setting requires restart", example = "true")
    private Boolean requiresRestart;

    @Schema(description = "Whether setting has been modified from default", example = "true")
    private Boolean isModified;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "User who last updated the setting", example = "admin")
    private String updatedBy;

    /**
     * Convert SystemSetting entity to SettingResponse DTO
     */
    public static SettingResponse fromEntity(SystemSetting setting) {
        return SettingResponse.builder()
            .id(setting.getId())
            .settingKey(setting.getSettingKey())
            .settingValue(setting.getIsSensitive() ? "***HIDDEN***" : setting.getSettingValue())
            .settingType(setting.getSettingType().name())
            .category(setting.getCategory().name())
            .displayName(setting.getDisplayName())
            .description(setting.getDescription())
            .defaultValue(setting.getIsSensitive() ? "***HIDDEN***" : setting.getDefaultValue())
            .validationRegex(setting.getValidationRegex())
            .minValue(setting.getMinValue())
            .maxValue(setting.getMaxValue())
            .allowedValues(parseAllowedValues(setting.getAllowedValues()))
            .isSensitive(setting.getIsSensitive())
            .isReadonly(setting.getIsReadonly())
            .requiresRestart(setting.getRequiresRestart())
            .isModified(setting.isModified())
            .createdAt(setting.getCreatedAt())
            .updatedAt(setting.getUpdatedAt())
            .updatedBy(setting.getUpdatedBy())
            .build();
    }

    /**
     * Convert SystemSetting entity to SettingResponse DTO (admin view with sensitive data)
     */
    public static SettingResponse fromEntityAdmin(SystemSetting setting) {
        return SettingResponse.builder()
            .id(setting.getId())
            .settingKey(setting.getSettingKey())
            .settingValue(setting.getSettingValue())
            .settingType(setting.getSettingType().name())
            .category(setting.getCategory().name())
            .displayName(setting.getDisplayName())
            .description(setting.getDescription())
            .defaultValue(setting.getDefaultValue())
            .validationRegex(setting.getValidationRegex())
            .minValue(setting.getMinValue())
            .maxValue(setting.getMaxValue())
            .allowedValues(parseAllowedValues(setting.getAllowedValues()))
            .isSensitive(setting.getIsSensitive())
            .isReadonly(setting.getIsReadonly())
            .requiresRestart(setting.getRequiresRestart())
            .isModified(setting.isModified())
            .createdAt(setting.getCreatedAt())
            .updatedAt(setting.getUpdatedAt())
            .updatedBy(setting.getUpdatedBy())
            .build();
    }

    /**
     * Parse allowed values JSON string to list
     */
    private static List<String> parseAllowedValues(String allowedValuesJson) {
        if (allowedValuesJson == null || allowedValuesJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Simple JSON array parsing for comma-separated values
            if (allowedValuesJson.startsWith("[") && allowedValuesJson.endsWith("]")) {
                String content = allowedValuesJson.substring(1, allowedValuesJson.length() - 1);
                return List.of(content.split(","));
            }
            return List.of(allowedValuesJson.split(","));
        } catch (Exception e) {
            return null;
        }
    }
}
