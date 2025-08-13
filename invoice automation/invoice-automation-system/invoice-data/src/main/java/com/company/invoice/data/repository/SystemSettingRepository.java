package com.company.invoice.data.repository;

import com.company.invoice.data.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SystemSetting entity operations
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * Find setting by key
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);

    /**
     * Find settings by category
     */
    List<SystemSetting> findByCategoryOrderByDisplayNameAsc(SystemSetting.SettingCategory category);

    /**
     * Find settings by type
     */
    List<SystemSetting> findBySettingTypeOrderByDisplayNameAsc(SystemSetting.SettingType settingType);

    /**
     * Find non-sensitive settings (for public API)
     */
    List<SystemSetting> findByIsSensitiveFalseOrderByCategoryAscDisplayNameAsc();

    /**
     * Find modified settings (different from default)
     */
    @Query("SELECT s FROM SystemSetting s WHERE " +
           "(s.defaultValue IS NULL AND s.settingValue IS NOT NULL) OR " +
           "(s.defaultValue IS NOT NULL AND s.settingValue IS NULL) OR " +
           "(s.defaultValue != s.settingValue) " +
           "ORDER BY s.category ASC, s.displayName ASC")
    List<SystemSetting> findModifiedSettings();

    /**
     * Find readonly settings
     */
    List<SystemSetting> findByIsReadonlyTrueOrderByDisplayNameAsc();

    /**
     * Find settings requiring restart
     */
    List<SystemSetting> findByRequiresRestartTrueOrderByDisplayNameAsc();

    /**
     * Check if setting key exists
     */
    boolean existsBySettingKey(String settingKey);

    /**
     * Update setting value
     */
    @Modifying
    @Query("UPDATE SystemSetting s SET " +
           "s.settingValue = :value, " +
           "s.updatedAt = :updatedAt, " +
           "s.updatedBy = :updatedBy " +
           "WHERE s.settingKey = :key")
    int updateSettingValue(@Param("key") String settingKey,
                          @Param("value") String settingValue,
                          @Param("updatedAt") LocalDateTime updatedAt,
                          @Param("updatedBy") String updatedBy);

    /**
     * Reset setting to default value
     */
    @Modifying
    @Query("UPDATE SystemSetting s SET " +
           "s.settingValue = s.defaultValue, " +
           "s.updatedAt = :updatedAt, " +
           "s.updatedBy = :updatedBy " +
           "WHERE s.settingKey = :key")
    int resetSettingToDefault(@Param("key") String settingKey,
                             @Param("updatedAt") LocalDateTime updatedAt,
                             @Param("updatedBy") String updatedBy);

    /**
     * Count settings by category
     */
    @Query("SELECT s.category, COUNT(s) FROM SystemSetting s GROUP BY s.category")
    List<Object[]> countSettingsByCategory();

    /**
     * Count modified settings by category
     */
    @Query("SELECT s.category, COUNT(s) FROM SystemSetting s WHERE " +
           "(s.defaultValue IS NULL AND s.settingValue IS NOT NULL) OR " +
           "(s.defaultValue IS NOT NULL AND s.settingValue IS NULL) OR " +
           "(s.defaultValue != s.settingValue) " +
           "GROUP BY s.category")
    List<Object[]> countModifiedSettingsByCategory();

    /**
     * Find settings updated after specific date
     */
    List<SystemSetting> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime since);

    /**
     * Find settings by key pattern (for search)
     */
    @Query("SELECT s FROM SystemSetting s WHERE " +
           "LOWER(s.settingKey) LIKE LOWER(CONCAT('%', :pattern, '%')) OR " +
           "LOWER(s.displayName) LIKE LOWER(CONCAT('%', :pattern, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :pattern, '%')) " +
           "ORDER BY s.category ASC, s.displayName ASC")
    List<SystemSetting> findByKeyPattern(@Param("pattern") String pattern);
}
