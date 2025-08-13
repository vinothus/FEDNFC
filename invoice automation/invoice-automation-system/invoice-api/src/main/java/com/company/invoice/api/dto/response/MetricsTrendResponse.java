package com.company.invoice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for metrics trend data (amounts, processing times, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsTrendResponse {
    
    private String metricType; // AMOUNT, PROCESSING_TIME, etc.
    private String title;
    private String description;
    private String unit; // USD, minutes, percentage, etc.
    private TrendDataResponse.Granularity granularity;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MetricDataPoint> data;
    private MetricsSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDataPoint {
        private String date; // Formatted date string for display
        private LocalDate rawDate; // Actual date for sorting/filtering
        private Double value; // Primary metric value
        private Double average; // Average for the period
        private Double minimum; // Minimum value
        private Double maximum; // Maximum value
        private Long count; // Number of items
        private Map<String, Object> breakdown; // Additional breakdown data
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private Double totalValue;
        private Double averageValue;
        private Double minimumValue;
        private Double maximumValue;
        private Double standardDeviation;
        private TrendDataResponse.TrendDirection trend;
        private Double trendPercentage; // Percentage change from start to end
        private String interpretation; // Human-readable interpretation
        private List<Insight> insights; // Key insights about the data
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String type; // POSITIVE, NEGATIVE, NEUTRAL, WARNING
        private String title;
        private String description;
        private Double impact; // Quantified impact if applicable
        private String recommendation; // Suggested action
    }
    
    public enum MetricType {
        AMOUNT("Total Amount", "USD"),
        PROCESSING_TIME("Processing Time", "minutes"),
        SUCCESS_RATE("Success Rate", "percentage"),
        VOLUME("Invoice Volume", "count"),
        AVERAGE_AMOUNT("Average Amount", "USD"),
        THROUGHPUT("Throughput", "invoices/hour");
        
        private final String displayName;
        private final String defaultUnit;
        
        MetricType(String displayName, String defaultUnit) {
            this.displayName = displayName;
            this.defaultUnit = defaultUnit;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDefaultUnit() {
            return defaultUnit;
        }
    }
}
