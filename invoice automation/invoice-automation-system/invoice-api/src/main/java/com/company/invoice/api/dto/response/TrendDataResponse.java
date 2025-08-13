package com.company.invoice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for trend data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataResponse {
    
    private String title;
    private String description;
    private Granularity granularity;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TrendDataPoint> data;
    private TrendSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date; // Formatted date string for display
        private LocalDate rawDate; // Actual date for sorting/filtering
        private Long totalInvoices;
        private Long completed;
        private Long pending;
        private Long processing;
        private Long failed;
        private Double amount;
        private Double averageProcessingTime; // in minutes
        private Double successRate; // percentage
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummary {
        private Long totalInvoices;
        private Double totalAmount;
        private Double averageSuccessRate;
        private Double averageProcessingTime;
        private TrendDirection invoiceTrend;
        private TrendDirection amountTrend;
        private TrendDirection successRateTrend;
        private TrendDirection processingTimeTrend;
        private String period;
    }
    
    public enum Granularity {
        DAILY("Daily"),
        WEEKLY("Weekly"), 
        MONTHLY("Monthly");
        
        private final String displayName;
        
        Granularity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum TrendDirection {
        UP("Increasing"),
        DOWN("Decreasing"),
        STABLE("Stable"),
        UNKNOWN("Unknown");
        
        private final String description;
        
        TrendDirection(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
