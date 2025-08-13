package com.company.invoice.data.repository;

import com.company.invoice.data.entity.InvoicePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing dynamic invoice patterns
 */
@Repository
public interface InvoicePatternRepository extends JpaRepository<InvoicePattern, Long> {

    /**
     * Find all active patterns by category, ordered by priority (lower number = higher priority)
     */
    @Query("SELECT p FROM InvoicePattern p " +
           "WHERE p.patternCategory = :category " +
           "AND p.isActive = true " +
           "ORDER BY p.patternPriority ASC, p.id ASC")
    List<InvoicePattern> findActivePatternssByCategory(@Param("category") InvoicePattern.PatternCategory category);

    /**
     * Find all active patterns ordered by category and priority
     */
    @Query("SELECT p FROM InvoicePattern p " +
           "WHERE p.isActive = true " +
           "ORDER BY p.patternCategory ASC, p.patternPriority ASC, p.id ASC")
    List<InvoicePattern> findAllActivePatternsOrderedByPriority();

    /**
     * Find active patterns by category with minimum confidence weight
     */
    @Query("SELECT p FROM InvoicePattern p " +
           "WHERE p.patternCategory = :category " +
           "AND p.isActive = true " +
           "AND p.confidenceWeight >= :minConfidence " +
           "ORDER BY p.patternPriority ASC, p.confidenceWeight DESC")
    List<InvoicePattern> findActivePatternsWithMinConfidence(
            @Param("category") InvoicePattern.PatternCategory category,
            @Param("minConfidence") java.math.BigDecimal minConfidence);

    /**
     * Find pattern by name (case-insensitive)
     */
    @Query("SELECT p FROM InvoicePattern p WHERE LOWER(p.patternName) = LOWER(:name)")
    Optional<InvoicePattern> findByPatternNameIgnoreCase(@Param("name") String name);

    /**
     * Find all patterns containing specific regex (for debugging/searching)
     */
    @Query("SELECT p FROM InvoicePattern p WHERE p.patternRegex LIKE %:regex%")
    List<InvoicePattern> findPatternsContainingRegex(@Param("regex") String regex);

    /**
     * Count active patterns by category
     */
    @Query("SELECT COUNT(p) FROM InvoicePattern p " +
           "WHERE p.patternCategory = :category AND p.isActive = true")
    long countActivePatternssByCategory(@Param("category") InvoicePattern.PatternCategory category);

    /**
     * Find patterns created by specific user
     */
    List<InvoicePattern> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find patterns with high priority (priority <= threshold)
     */
    @Query("SELECT p FROM InvoicePattern p " +
           "WHERE p.isActive = true " +
           "AND p.patternPriority <= :maxPriority " +
           "ORDER BY p.patternCategory ASC, p.patternPriority ASC")
    List<InvoicePattern> findHighPriorityPatterns(@Param("maxPriority") Integer maxPriority);

    /**
     * Find patterns for specific date formats (for DATE category)
     */
    @Query("SELECT p FROM InvoicePattern p " +
           "WHERE p.patternCategory = 'DATE' " +
           "AND p.isActive = true " +
           "AND p.dateFormat = :dateFormat " +
           "ORDER BY p.patternPriority ASC")
    List<InvoicePattern> findDatePatternssByFormat(@Param("dateFormat") String dateFormat);

    /**
     * Disable patterns by category (soft delete)
     */
    @Query("UPDATE InvoicePattern p SET p.isActive = false " +
           "WHERE p.patternCategory = :category")
    @org.springframework.data.jpa.repository.Modifying
    int disablePatternsByCategory(@Param("category") InvoicePattern.PatternCategory category);

    /**
     * Update pattern priority
     */
    @Query("UPDATE InvoicePattern p SET p.patternPriority = :priority " +
           "WHERE p.id = :id")
    @org.springframework.data.jpa.repository.Modifying
    int updatePatternPriority(@Param("id") Long id, @Param("priority") Integer priority);

    /**
     * Get pattern statistics by category
     */
    @Query("SELECT new map(" +
           "p.patternCategory as category, " +
           "COUNT(p) as totalCount, " +
           "SUM(CASE WHEN p.isActive = true THEN 1 ELSE 0 END) as activeCount, " +
           "AVG(p.confidenceWeight) as avgConfidence, " +
           "MIN(p.patternPriority) as highestPriority) " +
           "FROM InvoicePattern p " +
           "GROUP BY p.patternCategory " +
           "ORDER BY p.patternCategory")
    List<java.util.Map<String, Object>> getPatternStatisticsByCategory();

    /**
     * Find patterns that might be duplicates (same category and similar regex)
     * TEMPORARILY DISABLED DUE TO JPQL ISSUES
     */
    // @Query("SELECT p1 FROM InvoicePattern p1, InvoicePattern p2 " +
    //        "WHERE p1.patternCategory = p2.patternCategory " +
    //        "AND p1.id < p2.id " +
    //        "AND (p1.patternRegex = p2.patternRegex " +
    //        "     OR p1.patternName = p2.patternName)")
    // List<InvoicePattern> findPotentialDuplicates();

    /**
     * Custom query to check if a pattern with similar regex already exists
     * TEMPORARILY DISABLED - Using simple alternative
     */
    // @Query("SELECT COUNT(p) FROM InvoicePattern p " +
    //        "WHERE p.patternCategory = :category " +
    //        "AND p.patternRegex = :regex " +
    //        "AND (:excludeId IS NULL OR p.id != :excludeId)")
    // long countSimilarPatterns(@Param("category") InvoicePattern.PatternCategory category,
    //                          @Param("regex") String regex,
    //                          @Param("excludeId") Long excludeId);

    /**
     * Check if similar pattern exists (helper method)
     * TEMPORARILY SIMPLIFIED
     */
    default boolean existsSimilarPattern(InvoicePattern.PatternCategory category, String regex, Long excludeId) {
        // Simplified implementation - just check by category for now
        List<InvoicePattern> patterns = findActivePatternssByCategory(category);
        return patterns.stream()
               .filter(p -> excludeId == null || !p.getId().equals(excludeId))
               .anyMatch(p -> p.getPatternRegex().equals(regex));
    }

    /**
     * Check if pattern name exists
     */
    boolean existsByPatternName(String patternName);

    /**
     * Find patterns by category ordered by priority desc
     */
    List<InvoicePattern> findByPatternCategoryOrderByPatternPriorityDesc(InvoicePattern.PatternCategory category);

    /**
     * Find active patterns ordered by priority desc
     */
    List<InvoicePattern> findByIsActiveTrueOrderByPatternPriorityDesc();
}
