package com.company.invoice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard summary response containing key metrics and recent activity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard summary with key metrics and recent activity")
public class DashboardSummaryResponse {

    @Schema(description = "Overall statistics")
    private OverallStats overallStats;

    @Schema(description = "Processing statistics for different time periods")
    private ProcessingStats processingStats;

    @Schema(description = "Recent invoices processed")
    private List<RecentInvoiceResponse> recentInvoices;

    @Schema(description = "Processing status breakdown")
    private StatusBreakdown statusBreakdown;

    @Schema(description = "System health information")
    private SystemHealth systemHealth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Overall system statistics")
    public static class OverallStats {
        @Schema(description = "Total invoices processed", example = "1247")
        private Long totalInvoices;

        @Schema(description = "Total amount processed", example = "125487.50")
        private BigDecimal totalAmount;

        @Schema(description = "Success rate percentage", example = "92.5")
        private Double successRate;

        @Schema(description = "Average processing time in seconds", example = "4.2")
        private Double avgProcessingTime;

        @Schema(description = "Total unique vendors", example = "87")
        private Long totalVendors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Processing statistics for different time periods")
    public static class ProcessingStats {
        @Schema(description = "Invoices processed today", example = "23")
        private Long todayCount;

        @Schema(description = "Invoices processed this week", example = "156")
        private Long weekCount;

        @Schema(description = "Invoices processed this month", example = "687")
        private Long monthCount;

        @Schema(description = "Amount processed today", example = "12547.80")
        private BigDecimal todayAmount;

        @Schema(description = "Amount processed this week", example = "89234.50")
        private BigDecimal weekAmount;

        @Schema(description = "Amount processed this month", example = "345678.90")
        private BigDecimal monthAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Processing status breakdown")
    public static class StatusBreakdown {
        @Schema(description = "Successfully processed invoices", example = "1150")
        private Long completed;

        @Schema(description = "Invoices currently being processed", example = "15")
        private Long inProgress;

        @Schema(description = "Failed processing invoices", example = "82")
        private Long failed;

        @Schema(description = "Pending review invoices", example = "5")
        private Long pendingReview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "System health information")
    public static class SystemHealth {
        @Schema(description = "Email scheduler status", example = "RUNNING")
        private String emailSchedulerStatus;

        @Schema(description = "Last email check time")
        private LocalDateTime lastEmailCheck;

        @Schema(description = "OCR processing queue size", example = "3")
        private Integer ocrQueueSize;

        @Schema(description = "Database connection status", example = "HEALTHY")
        private String databaseStatus;

        @Schema(description = "Average OCR confidence", example = "94.2")
        private Double avgOcrConfidence;
    }
}
