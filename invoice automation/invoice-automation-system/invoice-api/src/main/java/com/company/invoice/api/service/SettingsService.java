package com.company.invoice.api.service;

import com.company.invoice.api.dto.request.SettingUpdateRequest;
import com.company.invoice.api.dto.response.SettingResponse;
import com.company.invoice.data.entity.SystemSetting;
import com.company.invoice.data.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing system settings
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SettingsService {

    private final SystemSettingRepository settingRepository;

    /**
     * Get all settings (non-sensitive for regular users)
     */
    public List<SettingResponse> getAllSettings() {
        log.info("📋 Fetching all settings");

        List<SystemSetting> settings = settingRepository.findAll();
        
        return settings.stream()
            .map(SettingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all settings for admin (including sensitive data)
     */
    public List<SettingResponse> getAllSettingsAdmin() {
        log.info("🔧 Admin: Fetching all settings including sensitive data");

        List<SystemSetting> settings = settingRepository.findAll();
        
        return settings.stream()
            .map(SettingResponse::fromEntityAdmin)
            .collect(Collectors.toList());
    }

    /**
     * Get settings by category
     */
    public List<SettingResponse> getSettingsByCategory(SystemSetting.SettingCategory category, boolean isAdmin) {
        log.info("📂 Fetching settings by category: {}", category);

        List<SystemSetting> settings = settingRepository.findByCategoryOrderByDisplayNameAsc(category);
        
        return settings.stream()
            .map(isAdmin ? SettingResponse::fromEntityAdmin : SettingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get setting by key
     */
    public Optional<SettingResponse> getSettingByKey(String key, boolean isAdmin) {
        log.info("🔍 Fetching setting by key: {}", key);

        return settingRepository.findBySettingKey(key)
            .map(isAdmin ? SettingResponse::fromEntityAdmin : SettingResponse::fromEntity);
    }

    /**
     * Update setting value
     */
    public Optional<SettingResponse> updateSetting(String key, SettingUpdateRequest request, String updatedBy) {
        log.info("✏️ Updating setting: {} by user: {}", key, updatedBy);

        return settingRepository.findBySettingKey(key)
            .map(setting -> {
                // Check if setting is readonly
                if (setting.getIsReadonly()) {
                    throw new IllegalArgumentException("Setting '" + key + "' is read-only and cannot be modified");
                }

                // Validate the new value
                validateSettingValue(setting, request.getSettingValue());

                // Update setting
                setting.setSettingValue(request.getSettingValue());
                setting.setUpdatedAt(LocalDateTime.now());
                setting.setUpdatedBy(updatedBy);

                SystemSetting savedSetting = settingRepository.save(setting);

                log.info("✅ Setting updated successfully: {} = {}", key, 
                    setting.getIsSensitive() ? "***HIDDEN***" : request.getSettingValue());

                return SettingResponse.fromEntityAdmin(savedSetting);
            });
    }

    /**
     * Reset setting to default value
     */
    public Optional<SettingResponse> resetSettingToDefault(String key, String updatedBy) {
        log.info("🔄 Resetting setting to default: {} by user: {}", key, updatedBy);

        return settingRepository.findBySettingKey(key)
            .map(setting -> {
                // Check if setting is readonly
                if (setting.getIsReadonly()) {
                    throw new IllegalArgumentException("Setting '" + key + "' is read-only and cannot be reset");
                }

                setting.setSettingValue(setting.getDefaultValue());
                setting.setUpdatedAt(LocalDateTime.now());
                setting.setUpdatedBy(updatedBy);

                SystemSetting savedSetting = settingRepository.save(setting);

                log.info("✅ Setting reset to default: {} = {}", key,
                    setting.getIsSensitive() ? "***HIDDEN***" : setting.getDefaultValue());

                return SettingResponse.fromEntityAdmin(savedSetting);
            });
    }

    /**
     * Get modified settings
     */
    public List<SettingResponse> getModifiedSettings(boolean isAdmin) {
        log.info("📋 Fetching modified settings");

        List<SystemSetting> settings = settingRepository.findModifiedSettings();
        
        return settings.stream()
            .map(isAdmin ? SettingResponse::fromEntityAdmin : SettingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Search settings
     */
    public List<SettingResponse> searchSettings(String query, boolean isAdmin) {
        log.info("🔍 Searching settings with query: {}", query);

        List<SystemSetting> settings = settingRepository.findByKeyPattern(query);
        
        return settings.stream()
            .map(isAdmin ? SettingResponse::fromEntityAdmin : SettingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get settings requiring restart
     */
    public List<SettingResponse> getSettingsRequiringRestart(boolean isAdmin) {
        log.info("🔄 Fetching settings requiring restart");

        List<SystemSetting> settings = settingRepository.findByRequiresRestartTrueOrderByDisplayNameAsc();
        
        return settings.stream()
            .filter(SystemSetting::isModified) // Only modified settings requiring restart
            .map(isAdmin ? SettingResponse::fromEntityAdmin : SettingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get settings statistics
     */
    public Map<String, Object> getSettingsStatistics() {
        log.info("📊 Generating settings statistics");

        long totalSettings = settingRepository.count();
        List<SystemSetting> modifiedSettings = settingRepository.findModifiedSettings();
        List<SystemSetting> readonlySettings = settingRepository.findByIsReadonlyTrueOrderByDisplayNameAsc();
        List<SystemSetting> sensitiveSettings = settingRepository.findAll().stream()
            .filter(SystemSetting::getIsSensitive)
            .collect(Collectors.toList());

        // Settings by category
        List<Object[]> categoryCounts = settingRepository.countSettingsByCategory();
        Map<String, Long> settingsByCategory = categoryCounts.stream()
            .collect(Collectors.toMap(
                arr -> ((SystemSetting.SettingCategory) arr[0]).name(),
                arr -> (Long) arr[1]
            ));

        return Map.of(
            "totalSettings", totalSettings,
            "modifiedSettings", modifiedSettings.size(),
            "readonlySettings", readonlySettings.size(),
            "sensitiveSettings", sensitiveSettings.size(),
            "settingsByCategory", settingsByCategory,
            "lastModified", modifiedSettings.stream()
                .filter(s -> s.getUpdatedAt() != null)
                .map(SystemSetting::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null)
        );
    }

    /**
     * Initialize default settings
     */
    @Transactional
    public void initializeDefaultSettings() {
        log.info("🔧 Initializing default settings");

        createDefaultSettingsIfNotExists();
        
        log.info("✅ Default settings initialized");
    }

    /**
     * Validate setting value according to its constraints
     */
    private void validateSettingValue(SystemSetting setting, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting value cannot be null");
        }

        // Type-specific validation
        switch (setting.getSettingType()) {
            case INTEGER:
                try {
                    long longValue = Long.parseLong(value);
                    if (setting.getMinValue() != null && longValue < setting.getMinValue()) {
                        throw new IllegalArgumentException("Value must be at least " + setting.getMinValue());
                    }
                    if (setting.getMaxValue() != null && longValue > setting.getMaxValue()) {
                        throw new IllegalArgumentException("Value must be at most " + setting.getMaxValue());
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value must be a valid integer");
                }
                break;

            case DECIMAL:
                try {
                    double doubleValue = Double.parseDouble(value);
                    if (setting.getMinValue() != null && doubleValue < setting.getMinValue()) {
                        throw new IllegalArgumentException("Value must be at least " + setting.getMinValue());
                    }
                    if (setting.getMaxValue() != null && doubleValue > setting.getMaxValue()) {
                        throw new IllegalArgumentException("Value must be at most " + setting.getMaxValue());
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value must be a valid decimal number");
                }
                break;

            case BOOLEAN:
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("Value must be 'true' or 'false'");
                }
                break;

            case EMAIL:
                if (!isValidEmail(value)) {
                    throw new IllegalArgumentException("Value must be a valid email address");
                }
                break;

            case URL:
                if (!isValidUrl(value)) {
                    throw new IllegalArgumentException("Value must be a valid URL");
                }
                break;
        }

        // Regex validation
        if (setting.getValidationRegex() != null && !setting.getValidationRegex().trim().isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(setting.getValidationRegex());
                if (!pattern.matcher(value).matches()) {
                    throw new IllegalArgumentException("Value does not match required format");
                }
            } catch (Exception e) {
                log.warn("⚠️ Invalid validation regex for setting {}: {}", setting.getSettingKey(), e.getMessage());
            }
        }

        // Enum validation
        if (setting.getSettingType() == SystemSetting.SettingType.ENUM && setting.getAllowedValues() != null) {
            List<String> allowedValues = List.of(setting.getAllowedValues().split(","));
            if (!allowedValues.contains(value)) {
                throw new IllegalArgumentException("Value must be one of: " + String.join(", ", allowedValues));
            }
        }
    }

    /**
     * Create default settings if they don't exist
     */
    private void createDefaultSettingsIfNotExists() {
        createSettingIfNotExists("email.smtp.host", "localhost", SystemSetting.SettingType.STRING, 
            SystemSetting.SettingCategory.EMAIL, "SMTP Host", "SMTP server hostname for outgoing emails");
        
        createSettingIfNotExists("email.smtp.port", "587", SystemSetting.SettingType.INTEGER, 
            SystemSetting.SettingCategory.EMAIL, "SMTP Port", "SMTP server port number");
        
        createSettingIfNotExists("email.polling.interval", "60", SystemSetting.SettingType.INTEGER, 
            SystemSetting.SettingCategory.EMAIL, "Polling Interval", "Email polling interval in seconds");
        
        createSettingIfNotExists("ocr.confidence.threshold", "80.0", SystemSetting.SettingType.DECIMAL, 
            SystemSetting.SettingCategory.OCR, "OCR Confidence Threshold", "Minimum OCR confidence percentage");
        
        createSettingIfNotExists("security.jwt.expiration", "86400", SystemSetting.SettingType.INTEGER, 
            SystemSetting.SettingCategory.SECURITY, "JWT Expiration", "JWT token expiration time in seconds");
        
        createSettingIfNotExists("processing.max.concurrent", "5", SystemSetting.SettingType.INTEGER, 
            SystemSetting.SettingCategory.PERFORMANCE, "Max Concurrent Processing", "Maximum concurrent invoice processing");
    }

    /**
     * Create setting if it doesn't exist
     */
    private void createSettingIfNotExists(String key, String defaultValue, SystemSetting.SettingType type,
                                        SystemSetting.SettingCategory category, String displayName, String description) {
        if (!settingRepository.existsBySettingKey(key)) {
            SystemSetting setting = SystemSetting.builder()
                .settingKey(key)
                .settingValue(defaultValue)
                .settingType(type)
                .category(category)
                .displayName(displayName)
                .description(description)
                .defaultValue(defaultValue)
                .isSensitive(false)
                .isReadonly(false)
                .requiresRestart(false)
                .createdAt(LocalDateTime.now())
                .build();
            
            settingRepository.save(setting);
            log.debug("✅ Created default setting: {}", key);
        }
    }

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Simple URL validation
     */
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
