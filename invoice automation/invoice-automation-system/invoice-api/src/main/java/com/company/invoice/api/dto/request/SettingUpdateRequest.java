package com.company.invoice.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating system settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for updating system setting value")
public class SettingUpdateRequest {

    @NotBlank(message = "Setting value is required")
    @Schema(description = "New setting value", example = "smtp.example.com")
    private String settingValue;

    @Schema(description = "Comment about the change", example = "Updated for new email provider")
    private String changeComment;
}
