package com.company.invoice.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System settings entity for configurable application parameters
 */
@Entity
@Table(name = "system_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SettingType settingType;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SettingCategory category;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "allowed_values", columnDefinition = "TEXT")
    private String allowedValues; // JSON array for enum-like settings

    @Column(name = "is_sensitive")
    @Builder.Default
    private Boolean isSensitive = false;

    @Column(name = "is_readonly")
    @Builder.Default
    private Boolean isReadonly = false;

    @Column(name = "requires_restart")
    @Builder.Default
    private Boolean requiresRestart = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Setting data types
     */
    public enum SettingType {
        STRING("String"),
        INTEGER("Integer"),
        DECIMAL("Decimal"),
        BOOLEAN("Boolean"),
        JSON("JSON"),
        EMAIL("Email"),
        URL("URL"),
        PASSWORD("Password"),
        ENUM("Enumeration");

        private final String displayName;

        SettingType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Setting categories for organization
     */
    public enum SettingCategory {
        GENERAL("General"),
        EMAIL("Email Processing"),
        OCR("OCR Processing"),
        SECURITY("Security"),
        NOTIFICATIONS("Notifications"),
        PERFORMANCE("Performance"),
        INTEGRATION("Integrations"),
        LOGGING("Logging");

        private final String displayName;

        SettingCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PreUpdate
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get setting value as specific type
     */
    public String getStringValue() {
        return settingValue;
    }

    public Integer getIntegerValue() {
        try {
            return settingValue != null ? Integer.valueOf(settingValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDecimalValue() {
        try {
            return settingValue != null ? Double.valueOf(settingValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getBooleanValue() {
        return settingValue != null ? Boolean.valueOf(settingValue) : null;
    }

    /**
     * Check if setting has been modified from default
     */
    public boolean isModified() {
        if (defaultValue == null && settingValue == null) {
            return false;
        }
        if (defaultValue == null || settingValue == null) {
            return true;
        }
        return !defaultValue.equals(settingValue);
    }
}
