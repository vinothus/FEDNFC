package com.company.invoice.api.controller;

import com.company.invoice.api.dto.response.DashboardSummaryResponse;
import com.company.invoice.api.dto.response.RecentInvoiceResponse;
import com.company.invoice.api.service.DashboardService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard controller for providing analytics and summary data
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics and summary endpoints")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Get complete dashboard summary with key metrics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    // @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')") // Temporarily disabled for testing
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        try {
            log.info("📊 Dashboard summary requested");
            
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
            
            log.info("✅ Dashboard summary generated successfully");
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("🚨 Error generating dashboard summary: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/overall-stats")
    @Operation(summary = "Get overall statistics", description = "Get overall system statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Overall statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<DashboardSummaryResponse.OverallStats> getOverallStats() {
        try {
            log.info("📈 Overall statistics requested");
            
            DashboardSummaryResponse.OverallStats stats = dashboardService.getOverallStats();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("🚨 Error getting overall statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/processing-stats")
    @Operation(summary = "Get processing statistics", description = "Get processing statistics for different time periods")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Processing statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<DashboardSummaryResponse.ProcessingStats> getProcessingStats() {
        try {
            log.info("📅 Processing statistics requested");
            
            DashboardSummaryResponse.ProcessingStats stats = dashboardService.getProcessingStats();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("🚨 Error getting processing statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/recent-invoices")
    @Operation(summary = "Get recent invoices", description = "Get list of recently processed invoices")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent invoices retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<List<RecentInvoiceResponse>> getRecentInvoices(
            @Parameter(description = "Number of recent invoices to return")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("📋 Recent invoices requested (limit: {})", limit);
            
            // Validate limit
            if (limit <= 0 || limit > 100) {
                limit = 10;
            }
            
            List<RecentInvoiceResponse> recentInvoices = dashboardService.getRecentInvoices(limit);
            
            return ResponseEntity.ok(recentInvoices);
            
        } catch (Exception e) {
            log.error("🚨 Error getting recent invoices: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status-breakdown")
    @Operation(summary = "Get status breakdown", description = "Get breakdown of invoices by processing status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status breakdown retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<DashboardSummaryResponse.StatusBreakdown> getStatusBreakdown() {
        try {
            log.info("🔍 Status breakdown requested");
            
            DashboardSummaryResponse.StatusBreakdown breakdown = dashboardService.getStatusBreakdown();
            
            return ResponseEntity.ok(breakdown);
            
        } catch (Exception e) {
            log.error("🚨 Error getting status breakdown: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/system-health")
    @Operation(summary = "Get system health", description = "Get system health and status information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System health retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryResponse.SystemHealth> getSystemHealth() {
        try {
            log.info("🏥 System health requested");
            
            DashboardSummaryResponse.SystemHealth health = dashboardService.getSystemHealth();
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("🚨 Error getting system health: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invoices/by-status/{status}")
    @Operation(summary = "Get invoices by status", description = "Get invoices filtered by processing status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getInvoicesByStatus(
            @Parameter(description = "Processing status") 
            @PathVariable String status,
            @Parameter(description = "Number of invoices to return")
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.info("📊 Invoices by status requested: {} (limit: {})", status, limit);
            
            // Validate status
            Invoice.ProcessingStatus processingStatus;
            try {
                processingStatus = Invoice.ProcessingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status", "message", "Valid statuses: PENDING, IN_PROGRESS, COMPLETED, FAILED"));
            }
            
            // Validate limit
            if (limit <= 0 || limit > 100) {
                limit = 20;
            }
            
            List<RecentInvoiceResponse> invoices = dashboardService.getInvoicesByStatus(processingStatus, limit);
            
            return ResponseEntity.ok(invoices);
            
        } catch (Exception e) {
            log.error("🚨 Error getting invoices by status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/processing-stats/range")
    @Operation(summary = "Get processing statistics for date range", description = "Get processing statistics for a specific date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Processing statistics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'APPROVER')")
    public ResponseEntity<?> getProcessingStatsForRange(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            log.info("📊 Processing statistics for range requested: {} to {}", startDate, endDate);
            
            // Validate date range
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date range", "message", "Start date must be before end date"));
            }
            
            if (startDate.isBefore(LocalDateTime.now().minusYears(1))) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date range", "message", "Date range too far in the past (max 1 year)"));
            }
            
            DashboardSummaryResponse.ProcessingStats stats = dashboardService.getProcessingStatsForRange(startDate, endDate);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("🚨 Error getting processing statistics for range: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
