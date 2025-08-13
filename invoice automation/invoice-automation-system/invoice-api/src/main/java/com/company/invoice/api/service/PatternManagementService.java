package com.company.invoice.api.service;

import com.company.invoice.api.dto.request.PatternRequest;
import com.company.invoice.api.dto.response.PatternResponse;
import com.company.invoice.api.dto.response.PatternStatisticsResponse;
import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import com.company.invoice.email.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Service for managing invoice patterns with enhanced admin capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatternManagementService {

    private final InvoicePatternRepository patternRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Get all patterns with pagination and sorting
     */
    public Page<PatternResponse> getAllPatterns(int page, int size, String sortBy, String sortDir) {
        log.info("📋 Fetching patterns - page: {}, size: {}, sort: {} {}", page, size, sortBy, sortDir);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<InvoicePattern> patternPage = patternRepository.findAll(pageable);

        return patternPage.map(pattern -> {
            PatternResponse.PatternUsageStats stats = getPatternUsageStats(pattern.getId());
            return PatternResponse.fromEntityWithStats(pattern, stats);
        });
    }

    /**
     * Get patterns by category
     */
    public List<PatternResponse> getPatternsByCategory(InvoicePattern.PatternCategory category) {
        log.info("🔍 Fetching patterns by category: {}", category);

        List<InvoicePattern> patterns = patternRepository.findByPatternCategoryOrderByPatternPriorityDesc(category);

        return patterns.stream()
            .map(pattern -> {
                PatternResponse.PatternUsageStats stats = getPatternUsageStats(pattern.getId());
                return PatternResponse.fromEntityWithStats(pattern, stats);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get active patterns only
     */
    public List<PatternResponse> getActivePatterns() {
        log.info("✅ Fetching active patterns");

        List<InvoicePattern> patterns = patternRepository.findByIsActiveTrueOrderByPatternPriorityDesc();

        return patterns.stream()
            .map(PatternResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get pattern by ID
     */
    public Optional<PatternResponse> getPatternById(Long id) {
        log.info("🔍 Fetching pattern by ID: {}", id);

        return patternRepository.findById(id)
            .map(pattern -> {
                PatternResponse.PatternUsageStats stats = getPatternUsageStats(pattern.getId());
                return PatternResponse.fromEntityWithStats(pattern, stats);
            });
    }

    /**
     * Create new pattern
     */
    public PatternResponse createPattern(PatternRequest request) {
        log.info("➕ Creating new pattern: {}", request.getPatternName());

        // Validate regex pattern
        validateRegexPattern(request.getPatternRegex());

        // Check for duplicate pattern names
        if (patternRepository.existsByPatternName(request.getPatternName())) {
            throw new IllegalArgumentException("Pattern name already exists: " + request.getPatternName());
        }

        // Create new pattern entity
        InvoicePattern pattern = InvoicePattern.builder()
            .patternName(request.getPatternName())
            .patternCategory(request.getPatternCategory())
            .patternRegex(request.getPatternRegex())
            .patternPriority(request.getPatternPriority())
            .confidenceWeight(BigDecimal.valueOf(request.getConfidenceWeight().doubleValue()))
            .isActive(request.getIsActive())
            .patternFlags(parsePatternFlags(request.getPatternFlags()))
            .dateFormat(request.getDateFormat())
            .patternDescription(request.getPatternDescription())
            .notes(request.getNotes())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        InvoicePattern savedPattern = patternRepository.save(pattern);

        log.info("✅ Pattern created successfully: {} (ID: {})", savedPattern.getPatternName(), savedPattern.getId());

        return PatternResponse.fromEntity(savedPattern);
    }

    /**
     * Update existing pattern
     */
    public Optional<PatternResponse> updatePattern(Long id, PatternRequest request) {
        log.info("✏️ Updating pattern ID: {} with name: {}", id, request.getPatternName());

        return patternRepository.findById(id)
            .map(existingPattern -> {
                // Validate regex pattern
                validateRegexPattern(request.getPatternRegex());

                // Check for duplicate pattern names (excluding current pattern)
                if (!existingPattern.getPatternName().equals(request.getPatternName()) &&
                    patternRepository.existsByPatternName(request.getPatternName())) {
                    throw new IllegalArgumentException("Pattern name already exists: " + request.getPatternName());
                }

                // Update pattern fields
                existingPattern.setPatternName(request.getPatternName());
                existingPattern.setPatternCategory(request.getPatternCategory());
                existingPattern.setPatternRegex(request.getPatternRegex());
                existingPattern.setPatternPriority(request.getPatternPriority());
                existingPattern.setConfidenceWeight(BigDecimal.valueOf(request.getConfidenceWeight().doubleValue()));
                existingPattern.setIsActive(request.getIsActive());
                existingPattern.setPatternFlags(parsePatternFlags(request.getPatternFlags()));
                existingPattern.setDateFormat(request.getDateFormat());
                existingPattern.setPatternDescription(request.getPatternDescription());
                existingPattern.setNotes(request.getNotes());
                existingPattern.setUpdatedAt(LocalDateTime.now());

                InvoicePattern savedPattern = patternRepository.save(existingPattern);

                log.info("✅ Pattern updated successfully: {} (ID: {})", savedPattern.getPatternName(), savedPattern.getId());

                PatternResponse.PatternUsageStats stats = getPatternUsageStats(savedPattern.getId());
                return PatternResponse.fromEntityWithStats(savedPattern, stats);
            });
    }

    /**
     * Delete pattern
     */
    public boolean deletePattern(Long id) {
        log.info("🗑️ Deleting pattern ID: {}", id);

        return patternRepository.findById(id)
            .map(pattern -> {
                // Check if pattern is currently in use
                PatternResponse.PatternUsageStats stats = getPatternUsageStats(id);
                if (stats.getUsageCount() > 0) {
                    log.warn("⚠️ Cannot delete pattern {} - it has been used {} times", 
                        pattern.getPatternName(), stats.getUsageCount());
                    throw new IllegalStateException("Cannot delete pattern that has usage history. Consider deactivating instead.");
                }

                patternRepository.delete(pattern);
                log.info("✅ Pattern deleted successfully: {} (ID: {})", pattern.getPatternName(), id);
                return true;
            })
            .orElse(false);
    }

    /**
     * Activate/Deactivate pattern
     */
    public Optional<PatternResponse> togglePatternStatus(Long id, boolean active) {
        log.info("🔄 {} pattern ID: {}", active ? "Activating" : "Deactivating", id);

        return patternRepository.findById(id)
            .map(pattern -> {
                pattern.setIsActive(active);
                pattern.setUpdatedAt(LocalDateTime.now());

                InvoicePattern savedPattern = patternRepository.save(pattern);

                log.info("✅ Pattern {} successfully: {} (ID: {})", 
                    active ? "activated" : "deactivated", savedPattern.getPatternName(), savedPattern.getId());

                PatternResponse.PatternUsageStats stats = getPatternUsageStats(savedPattern.getId());
                return PatternResponse.fromEntityWithStats(savedPattern, stats);
            });
    }

    /**
     * Test pattern against sample text
     */
    public PatternTestResult testPattern(String regex, String sampleText, String flags) {
        log.info("🧪 Testing pattern against sample text");

        try {
            // Validate and compile pattern
            validateRegexPattern(regex);
            
            int regexFlags = parsePatternFlags(flags);
            Pattern compiledPattern = Pattern.compile(regex, regexFlags);
            
            java.util.regex.Matcher matcher = compiledPattern.matcher(sampleText);
            
            PatternTestResult result = PatternTestResult.builder()
                .isValid(true)
                .matches(matcher.find())
                .build();

            if (result.isMatches()) {
                matcher.reset();
                if (matcher.find()) {
                    result.setMatchedText(matcher.group());
                    result.setStartIndex(matcher.start());
                    result.setEndIndex(matcher.end());
                    
                    // Get capture groups
                    if (matcher.groupCount() > 0) {
                        result.setCaptureGroups(new String[matcher.groupCount()]);
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            result.getCaptureGroups()[i-1] = matcher.group(i);
                        }
                    }
                }
            }

            log.info("✅ Pattern test completed - matches: {}", result.isMatches());
            return result;

        } catch (Exception e) {
            log.error("❌ Pattern test failed: {}", e.getMessage());
            return PatternTestResult.builder()
                .isValid(false)
                .matches(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Get pattern usage statistics
     */
    private PatternResponse.PatternUsageStats getPatternUsageStats(Long patternId) {
        // This is a simplified implementation
        // In a real system, you would track pattern usage in a separate table
        
        return PatternResponse.PatternUsageStats.builder()
            .usageCount(0L) // Would be calculated from usage tracking
            .successRate(0.0) // Would be calculated from success/failure tracking
            .lastUsed(null) // Would be the last usage timestamp
            .avgConfidence(0.0) // Would be calculated from confidence scores
            .build();
    }

    /**
     * Validate regex pattern syntax
     */
    private void validateRegexPattern(String regex) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
        }
    }

    /**
     * Parse pattern flags string to integer flags
     */
    private int parsePatternFlags(String flags) {
        if (flags == null || flags.trim().isEmpty()) {
            return 0;
        }

        int regexFlags = 0;
        String[] flagArray = flags.split(",");
        
        for (String flag : flagArray) {
            switch (flag.trim().toUpperCase()) {
                case "CASE_INSENSITIVE":
                    regexFlags |= Pattern.CASE_INSENSITIVE;
                    break;
                case "MULTILINE":
                    regexFlags |= Pattern.MULTILINE;
                    break;
                case "DOTALL":
                    regexFlags |= Pattern.DOTALL;
                    break;
                case "UNICODE_CASE":
                    regexFlags |= Pattern.UNICODE_CASE;
                    break;
                default:
                    log.warn("⚠️ Unknown pattern flag: {}", flag);
                    break;
            }
        }
        
        return regexFlags;
    }

    /**
     * Get comprehensive pattern statistics
     */
    public PatternStatisticsResponse getPatternStatistics() {
        log.info("📊 Generating pattern statistics");
        
        // Get all patterns
        List<InvoicePattern> allPatterns = patternRepository.findAll();
        
        // Overall statistics
        long totalPatterns = allPatterns.size();
        long activePatterns = allPatterns.stream().mapToLong(p -> p.getIsActive() ? 1 : 0).sum();
        long goldenPatterns = allPatterns.stream()
            .mapToLong(p -> "SYSTEM_GOLDEN".equals(p.getCreatedBy()) ? 1 : 0).sum();
        
        double avgConfidence = allPatterns.stream()
            .filter(p -> p.getConfidenceWeight() != null)
            .mapToDouble(p -> p.getConfidenceWeight().doubleValue())
            .average().orElse(0.0);
        
        PatternStatisticsResponse.OverallStats overall = PatternStatisticsResponse.OverallStats.builder()
            .totalPatterns(totalPatterns)
            .activePatterns(activePatterns)
            .inactivePatterns(totalPatterns - activePatterns)
            .goldenPatterns(goldenPatterns)
            .userCreatedPatterns(totalPatterns - goldenPatterns)
            .averageConfidenceWeight(avgConfidence)
            .lastUpdated(LocalDateTime.now())
            .build();
        
        // Category breakdown
        Map<InvoicePattern.PatternCategory, List<InvoicePattern>> patternsByCategory = 
            allPatterns.stream().collect(Collectors.groupingBy(InvoicePattern::getPatternCategory));
        
        List<PatternStatisticsResponse.CategoryStats> categoryStats = patternsByCategory.entrySet().stream()
            .map(entry -> {
                InvoicePattern.PatternCategory category = entry.getKey();
                List<InvoicePattern> patterns = entry.getValue();
                
                long totalCount = patterns.size();
                long activeCount = patterns.stream().mapToLong(p -> p.getIsActive() ? 1 : 0).sum();
                long goldenCount = patterns.stream()
                    .mapToLong(p -> "SYSTEM_GOLDEN".equals(p.getCreatedBy()) ? 1 : 0).sum();
                
                double avgPriority = patterns.stream()
                    .filter(p -> p.getPatternPriority() != null)
                    .mapToDouble(p -> p.getPatternPriority().doubleValue())
                    .average().orElse(0.0);
                
                double avgConfidenceCategory = patterns.stream()
                    .filter(p -> p.getConfidenceWeight() != null)
                    .mapToDouble(p -> p.getConfidenceWeight().doubleValue())
                    .average().orElse(0.0);
                
                // Find most used pattern (for now, use highest priority as proxy)
                String topPattern = patterns.stream()
                    .filter(InvoicePattern::getIsActive)
                    .min(Comparator.comparing(InvoicePattern::getPatternPriority))
                    .map(InvoicePattern::getPatternName)
                    .orElse("None");
                
                return PatternStatisticsResponse.CategoryStats.builder()
                    .category(category.name())
                    .totalCount(totalCount)
                    .activeCount(activeCount)
                    .inactiveCount(totalCount - activeCount)
                    .goldenCount(goldenCount)
                    .averagePriority(avgPriority)
                    .averageConfidence(avgConfidenceCategory)
                    .topPattern(topPattern)
                    .build();
            })
            .collect(Collectors.toList());
        
        // Golden data status
        // Expected patterns based on DataInitializer - we know there should be 12 patterns
        int expectedGoldenPatterns = 12; // From DataInitializer.java
        PatternStatisticsResponse.GoldenDataStatus goldenDataStatus = analyzeGoldenDataStatus(
            allPatterns, expectedGoldenPatterns);
        
        // Pattern health analysis
        List<PatternStatisticsResponse.PatternHealth> patternHealth = analyzePatternHealth(allPatterns);
        
        // Recommendations
        List<PatternStatisticsResponse.RecommendedAction> recommendations = 
            generateRecommendations(overall, categoryStats, goldenDataStatus, patternHealth);
        
        return PatternStatisticsResponse.builder()
            .overall(overall)
            .categoryBreakdown(categoryStats)
            .goldenData(goldenDataStatus)
            .patternHealth(patternHealth)
            .recommendations(recommendations)
            .build();
    }
    
    private PatternStatisticsResponse.GoldenDataStatus analyzeGoldenDataStatus(
            List<InvoicePattern> allPatterns, int expectedGoldenPatterns) {
        
        List<InvoicePattern> goldenPatterns = allPatterns.stream()
            .filter(p -> "SYSTEM_GOLDEN".equals(p.getCreatedBy()))
            .collect(Collectors.toList());
        
        Set<String> availableCategories = goldenPatterns.stream()
            .map(p -> p.getPatternCategory().name())
            .collect(Collectors.toSet());
        
        Set<String> allPossibleCategories = Arrays.stream(InvoicePattern.PatternCategory.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        
        List<String> missingCategories = allPossibleCategories.stream()
            .filter(cat -> !availableCategories.contains(cat))
            .collect(Collectors.toList());
        
        boolean isComplete = goldenPatterns.size() >= expectedGoldenPatterns && missingCategories.isEmpty();
        String status = isComplete ? "COMPLETE" : 
            (goldenPatterns.size() < expectedGoldenPatterns / 2 ? "INCOMPLETE" : "NEEDS_REVIEW");
        
        return PatternStatisticsResponse.GoldenDataStatus.builder()
            .isComplete(isComplete)
            .expectedGoldenPatterns(expectedGoldenPatterns)
            .actualGoldenPatterns(goldenPatterns.size())
            .missingCategories(missingCategories)
            .availableCategories(new ArrayList<>(availableCategories))
            .lastGoldenUpdate(goldenPatterns.stream()
                .map(InvoicePattern::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null))
            .status(status)
            .build();
    }
    
    private List<PatternStatisticsResponse.PatternHealth> analyzePatternHealth(List<InvoicePattern> patterns) {
        List<PatternStatisticsResponse.PatternHealth> issues = new ArrayList<>();
        
        // Check for inactive patterns
        List<InvoicePattern> inactivePatterns = patterns.stream()
            .filter(p -> !p.getIsActive())
            .collect(Collectors.toList());
        
        if (!inactivePatterns.isEmpty()) {
            issues.add(PatternStatisticsResponse.PatternHealth.builder()
                .issue("Inactive Patterns")
                .severity("MEDIUM")
                .description("Some patterns are currently inactive and won't be used for extraction")
                .affectedPatterns(inactivePatterns.size())
                .patternNames(inactivePatterns.stream()
                    .map(InvoicePattern::getPatternName)
                    .limit(5)
                    .collect(Collectors.toList()))
                .build());
        }
        
        // Check for low confidence patterns
        List<InvoicePattern> lowConfidencePatterns = patterns.stream()
            .filter(p -> p.getConfidenceWeight() != null && 
                        p.getConfidenceWeight().doubleValue() < 0.5)
            .collect(Collectors.toList());
        
        if (!lowConfidencePatterns.isEmpty()) {
            issues.add(PatternStatisticsResponse.PatternHealth.builder()
                .issue("Low Confidence Patterns")
                .severity("HIGH")
                .description("Patterns with confidence weight below 0.5 may produce unreliable results")
                .affectedPatterns(lowConfidencePatterns.size())
                .patternNames(lowConfidencePatterns.stream()
                    .map(InvoicePattern::getPatternName)
                    .limit(5)
                    .collect(Collectors.toList()))
                .build());
        }
        
        return issues;
    }
    
    private List<PatternStatisticsResponse.RecommendedAction> generateRecommendations(
            PatternStatisticsResponse.OverallStats overall,
            List<PatternStatisticsResponse.CategoryStats> categoryStats,
            PatternStatisticsResponse.GoldenDataStatus goldenData,
            List<PatternStatisticsResponse.PatternHealth> health) {
        
        List<PatternStatisticsResponse.RecommendedAction> actions = new ArrayList<>();
        
        // Golden data recommendations
        if (!goldenData.getIsComplete()) {
            actions.add(PatternStatisticsResponse.RecommendedAction.builder()
                .action("Restore Golden Data")
                .priority("HIGH")
                .description("Some golden patterns are missing. Consider re-running data initialization.")
                .category("GOLDEN_DATA")
                .details(Map.of(
                    "missing_patterns", goldenData.getExpectedGoldenPatterns() - goldenData.getActualGoldenPatterns(),
                    "missing_categories", goldenData.getMissingCategories()
                ))
                .build());
        }
        
        // Performance recommendations
        if (overall.getActivePatterns() < overall.getTotalPatterns() * 0.8) {
            actions.add(PatternStatisticsResponse.RecommendedAction.builder()
                .action("Review Inactive Patterns")
                .priority("MEDIUM")
                .description("Many patterns are inactive. Review and activate useful patterns.")
                .category("PERFORMANCE")
                .details(Map.of("inactive_count", overall.getInactivePatterns()))
                .build());
        }
        
        return actions;
    }

    /**
     * Result of pattern testing
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PatternTestResult {
        private boolean isValid;
        private boolean matches;
        private String matchedText;
        private int startIndex;
        private int endIndex;
        private String[] captureGroups;
        private String errorMessage;
    }
}
