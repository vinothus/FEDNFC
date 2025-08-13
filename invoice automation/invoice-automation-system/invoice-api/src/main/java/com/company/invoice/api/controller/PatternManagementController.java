package com.company.invoice.api.controller;

import com.company.invoice.api.dto.request.PatternRequest;
import com.company.invoice.api.dto.response.PatternResponse;
import com.company.invoice.api.dto.response.PatternStatisticsResponse;
import com.company.invoice.api.service.PatternManagementService;
import com.company.invoice.data.entity.InvoicePattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Enhanced admin controller for invoice pattern management
 */
@RestController
@RequestMapping("/admin/patterns")
@Tag(name = "Pattern Management", description = "Admin endpoints for managing invoice extraction patterns")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class PatternManagementController {

    private final PatternManagementService patternManagementService;

    @GetMapping
    @Operation(summary = "Get all patterns", description = "Get all invoice patterns with pagination and sorting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patterns retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PatternResponse>> getAllPatterns(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "patternPriority") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            log.info("📋 Admin: Getting all patterns - page: {}, size: {}", page, size);
            
            Page<PatternResponse> patterns = patternManagementService.getAllPatterns(page, size, sortBy, sortDir);
            
            log.info("✅ Admin: Retrieved {} patterns", patterns.getTotalElements());
            return ResponseEntity.ok(patterns);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting patterns: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get patterns by category", description = "Get patterns filtered by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patterns retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPatternsByCategory(
            @Parameter(description = "Pattern category") @PathVariable String category) {
        try {
            log.info("🔍 Admin: Getting patterns by category: {}", category);
            
            // Validate category
            InvoicePattern.PatternCategory patternCategory;
            try {
                patternCategory = InvoicePattern.PatternCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid category", "message", "Valid categories: " + 
                        String.join(", ", getValidCategories())));
            }
            
            List<PatternResponse> patterns = patternManagementService.getPatternsByCategory(patternCategory);
            
            log.info("✅ Admin: Retrieved {} patterns for category: {}", patterns.size(), category);
            return ResponseEntity.ok(patterns);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting patterns by category: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active patterns", description = "Get all currently active patterns")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active patterns retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PatternResponse>> getActivePatterns() {
        try {
            log.info("✅ Admin: Getting active patterns");
            
            List<PatternResponse> patterns = patternManagementService.getActivePatterns();
            
            log.info("✅ Admin: Retrieved {} active patterns", patterns.size());
            return ResponseEntity.ok(patterns);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting active patterns: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get pattern statistics", description = "Get comprehensive statistics about patterns")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatternStatisticsResponse> getPatternStatistics() {
        try {
            log.info("📊 Admin: Getting pattern statistics");
            
            PatternStatisticsResponse stats = patternManagementService.getPatternStatistics();
            
            log.info("✅ Admin: Pattern statistics retrieved");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting pattern statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pattern by ID", description = "Get specific pattern by ID with usage statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Pattern not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatternResponse> getPatternById(
            @Parameter(description = "Pattern ID") @PathVariable Long id) {
        try {
            log.info("🔍 Admin: Getting pattern by ID: {}", id);
            
            return patternManagementService.getPatternById(id)
                .map(pattern -> {
                    log.info("✅ Admin: Retrieved pattern: {}", pattern.getPatternName());
                    return ResponseEntity.ok(pattern);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting pattern by ID: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create new pattern", description = "Create a new invoice extraction pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pattern created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pattern data"),
        @ApiResponse(responseCode = "409", description = "Pattern name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPattern(@Valid @RequestBody PatternRequest request) {
        try {
            log.info("➕ Admin: Creating new pattern: {}", request.getPatternName());
            
            PatternResponse pattern = patternManagementService.createPattern(request);
            
            log.info("✅ Admin: Pattern created successfully: {} (ID: {})", 
                pattern.getPatternName(), pattern.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(pattern);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Admin: Invalid pattern data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid pattern data", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error creating pattern: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create pattern", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pattern", description = "Update an existing invoice extraction pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pattern data"),
        @ApiResponse(responseCode = "404", description = "Pattern not found"),
        @ApiResponse(responseCode = "409", description = "Pattern name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePattern(
            @Parameter(description = "Pattern ID") @PathVariable Long id,
            @Valid @RequestBody PatternRequest request) {
        try {
            log.info("✏️ Admin: Updating pattern ID: {} with name: {}", id, request.getPatternName());
            
            return patternManagementService.updatePattern(id, request)
                .map(pattern -> {
                    log.info("✅ Admin: Pattern updated successfully: {} (ID: {})", 
                        pattern.getPatternName(), pattern.getId());
                    return ResponseEntity.ok(pattern);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (IllegalArgumentException e) {
            log.warn("❌ Admin: Invalid pattern data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid pattern data", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error updating pattern: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update pattern", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pattern", description = "Delete an invoice extraction pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pattern deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Pattern not found"),
        @ApiResponse(responseCode = "409", description = "Pattern has usage history"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePattern(
            @Parameter(description = "Pattern ID") @PathVariable Long id) {
        try {
            log.info("🗑️ Admin: Deleting pattern ID: {}", id);
            
            boolean deleted = patternManagementService.deletePattern(id);
            
            if (deleted) {
                log.info("✅ Admin: Pattern deleted successfully (ID: {})", id);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalStateException e) {
            log.warn("❌ Admin: Cannot delete pattern: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Cannot delete pattern", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error deleting pattern: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to delete pattern", "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle pattern status", description = "Activate or deactivate a pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern status updated successfully"),
        @ApiResponse(responseCode = "404", description = "Pattern not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatternResponse> togglePatternStatus(
            @Parameter(description = "Pattern ID") @PathVariable Long id,
            @Parameter(description = "Active status") @RequestParam boolean active) {
        try {
            log.info("🔄 Admin: {} pattern ID: {}", active ? "Activating" : "Deactivating", id);
            
            return patternManagementService.togglePatternStatus(id, active)
                .map(pattern -> {
                    log.info("✅ Admin: Pattern {} successfully: {} (ID: {})", 
                        active ? "activated" : "deactivated", pattern.getPatternName(), pattern.getId());
                    return ResponseEntity.ok(pattern);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("🚨 Admin: Error toggling pattern status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/test")
    @Operation(summary = "Test pattern", description = "Test a regex pattern against sample text")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pattern tested successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid regex pattern"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testPattern(@RequestBody Map<String, String> testRequest) {
        try {
            log.info("🧪 Admin: Testing pattern");
            
            String regex = testRequest.get("regex");
            String sampleText = testRequest.get("sampleText");
            String flags = testRequest.get("flags");
            
            if (regex == null || sampleText == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields", "message", "Both 'regex' and 'sampleText' are required"));
            }
            
            PatternManagementService.PatternTestResult result = patternManagementService.testPattern(regex, sampleText, flags);
            
            log.info("✅ Admin: Pattern test completed - matches: {}", result.isMatches());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error testing pattern: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to test pattern", "message", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "Get available categories", description = "Get list of available pattern categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAvailableCategories() {
        try {
            log.info("📋 Admin: Getting available pattern categories");
            
            List<String> categories = getValidCategories();
            
            return ResponseEntity.ok(categories);
            
        } catch (Exception e) {
            log.error("🚨 Admin: Error getting categories: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get valid pattern categories
     */
    private List<String> getValidCategories() {
        return List.of(
            InvoicePattern.PatternCategory.INVOICE_NUMBER.name(),
            InvoicePattern.PatternCategory.AMOUNT.name(),
            InvoicePattern.PatternCategory.DATE.name(),
            InvoicePattern.PatternCategory.VENDOR.name(),
            InvoicePattern.PatternCategory.CURRENCY.name()
        );
    }
}