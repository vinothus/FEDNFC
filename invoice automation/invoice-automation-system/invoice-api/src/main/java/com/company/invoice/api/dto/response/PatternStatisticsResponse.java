package com.company.invoice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for pattern statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternStatisticsResponse {
    
    private OverallStats overall;
    private List<CategoryStats> categoryBreakdown;
    private GoldenDataStatus goldenData;
    private List<PatternHealth> patternHealth;
    private List<RecommendedAction> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallStats {
        private Long totalPatterns;
        private Long activePatterns;
        private Long inactivePatterns;
        private Long goldenPatterns; // Created by SYSTEM_GOLDEN
        private Long userCreatedPatterns;
        private Double averageConfidenceWeight;
        private LocalDateTime lastUpdated;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String category;
        private Long totalCount;
        private Long activeCount;
        private Long inactiveCount;
        private Long goldenCount;
        private Double averagePriority;
        private Double averageConfidence;
        private String topPattern; // Most used pattern in this category
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoldenDataStatus {
        private Boolean isComplete;
        private Integer expectedGoldenPatterns;
        private Integer actualGoldenPatterns;
        private List<String> missingCategories;
        private List<String> availableCategories;
        private LocalDateTime lastGoldenUpdate;
        private String status; // "COMPLETE", "INCOMPLETE", "NEEDS_REVIEW"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternHealth {
        private String issue;
        private String severity; // "HIGH", "MEDIUM", "LOW"
        private String description;
        private Integer affectedPatterns;
        private List<String> patternNames;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedAction {
        private String action;
        private String priority; // "HIGH", "MEDIUM", "LOW"
        private String description;
        private String category;
        private Map<String, Object> details;
    }
}
