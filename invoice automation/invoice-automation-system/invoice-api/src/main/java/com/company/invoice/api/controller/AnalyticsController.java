package com.company.invoice.api.controller;

import com.company.invoice.api.dto.response.TrendDataResponse;
import com.company.invoice.api.dto.response.MetricsTrendResponse;
import com.company.invoice.api.dto.response.AnalyticsFilterResponse;
import com.company.invoice.api.service.AnalyticsService;
import com.company.invoice.email.entity.Invoice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Analytics controller for providing historical trend data and advanced metrics
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Advanced analytics and trend data endpoints")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/trends/processing")
    @Operation(summary = "Get invoice processing trends", description = "Get historical invoice processing trends with filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Processing trends retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getProcessingTrends(
            @Parameter(description = "Number of days to include (default: 30, max: 365)")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Start date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Granularity: DAILY, WEEKLY, MONTHLY")
            @RequestParam(defaultValue = "DAILY") String granularity,
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) String status) {
        try {
            log.info("📈 Processing trends requested - days: {}, granularity: {}, status: {}", days, granularity, status);
            
            // Validate parameters
            if (days <= 0 || days > 365) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid days parameter", "message", "Days must be between 1 and 365"));
            }
            
            // Validate granularity
            TrendDataResponse.Granularity gran;
            try {
                gran = TrendDataResponse.Granularity.valueOf(granularity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid granularity", "message", "Valid values: DAILY, WEEKLY, MONTHLY"));
            }
            
            // Validate status if provided
            Invoice.ProcessingStatus processingStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    processingStatus = Invoice.ProcessingStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status", "message", "Valid statuses: PENDING, IN_PROGRESS, COMPLETED, FAILED"));
                }
            }
            
            TrendDataResponse trends = analyticsService.getProcessingTrends(days, startDate, endDate, gran, processingStatus);
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            log.error("🚨 Error getting processing trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/trends/amounts")
    @Operation(summary = "Get amount trends", description = "Get historical invoice amount trends")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Amount trends retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getAmountTrends(
            @Parameter(description = "Number of days to include (default: 30, max: 365)")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Start date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Granularity: DAILY, WEEKLY, MONTHLY")
            @RequestParam(defaultValue = "DAILY") String granularity,
            @Parameter(description = "Currency filter (optional)")
            @RequestParam(required = false) String currency) {
        try {
            log.info("💰 Amount trends requested - days: {}, granularity: {}, currency: {}", days, granularity, currency);
            
            // Validate parameters
            if (days <= 0 || days > 365) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid days parameter", "message", "Days must be between 1 and 365"));
            }
            
            // Validate granularity
            TrendDataResponse.Granularity gran;
            try {
                gran = TrendDataResponse.Granularity.valueOf(granularity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid granularity", "message", "Valid values: DAILY, WEEKLY, MONTHLY"));
            }
            
            MetricsTrendResponse trends = analyticsService.getAmountTrends(days, startDate, endDate, gran, currency);
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            log.error("🚨 Error getting amount trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/trends/processing-time")
    @Operation(summary = "Get processing time trends", description = "Get historical processing time trends")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Processing time trends retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getProcessingTimeTrends(
            @Parameter(description = "Number of days to include (default: 30, max: 365)")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Start date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format, optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Granularity: DAILY, WEEKLY, MONTHLY")
            @RequestParam(defaultValue = "DAILY") String granularity) {
        try {
            log.info("⏱️ Processing time trends requested - days: {}, granularity: {}", days, granularity);
            
            // Validate parameters
            if (days <= 0 || days > 365) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid days parameter", "message", "Days must be between 1 and 365"));
            }
            
            // Validate granularity
            TrendDataResponse.Granularity gran;
            try {
                gran = TrendDataResponse.Granularity.valueOf(granularity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid granularity", "message", "Valid values: DAILY, WEEKLY, MONTHLY"));
            }
            
            MetricsTrendResponse trends = analyticsService.getProcessingTimeTrends(days, startDate, endDate, gran);
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            log.error("🚨 Error getting processing time trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/filters/available")
    @Operation(summary = "Get available filter options", description = "Get all available filter options for analytics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filter options retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<AnalyticsFilterResponse> getAvailableFilters() {
        try {
            log.info("🔍 Available filters requested");
            
            AnalyticsFilterResponse filters = analyticsService.getAvailableFilters();
            
            return ResponseEntity.ok(filters);
            
        } catch (Exception e) {
            log.error("🚨 Error getting available filters: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summary/advanced")
    @Operation(summary = "Get advanced analytics summary", description = "Get comprehensive analytics summary with trends")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advanced summary retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getAdvancedSummary(
            @Parameter(description = "Number of days to include (default: 30)")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Include trends data")
            @RequestParam(defaultValue = "true") boolean includeTrends) {
        try {
            log.info("📊 Advanced analytics summary requested - days: {}, includeTrends: {}", days, includeTrends);
            
            // Validate parameters
            if (days <= 0 || days > 365) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid days parameter", "message", "Days must be between 1 and 365"));
            }
            
            Map<String, Object> summary = analyticsService.getAdvancedSummary(days, includeTrends);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("🚨 Error getting advanced summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/export/trends")
    @Operation(summary = "Export trend data", description = "Export trend data in various formats")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trend data exported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> exportTrendData(
            @Parameter(description = "Export format: JSON, CSV")
            @RequestParam(defaultValue = "JSON") String format,
            @Parameter(description = "Number of days to include (default: 30)")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Data type: PROCESSING, AMOUNTS, PROCESSING_TIME")
            @RequestParam(defaultValue = "PROCESSING") String dataType) {
        try {
            log.info("📤 Export trend data requested - format: {}, days: {}, dataType: {}", format, days, dataType);
            
            // Validate parameters
            if (days <= 0 || days > 365) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid days parameter", "message", "Days must be between 1 and 365"));
            }
            
            if (!"JSON".equalsIgnoreCase(format) && !"CSV".equalsIgnoreCase(format)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid format", "message", "Valid formats: JSON, CSV"));
            }
            
            String exportData = analyticsService.exportTrendData(format.toUpperCase(), days, dataType.toUpperCase());
            
            return ResponseEntity.ok()
                .header("Content-Disposition", String.format("attachment; filename=trends_%d_days.%s", days, format.toLowerCase()))
                .header("Content-Type", "JSON".equalsIgnoreCase(format) ? "application/json" : "text/csv")
                .body(exportData);
            
        } catch (Exception e) {
            log.error("🚨 Error exporting trend data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
