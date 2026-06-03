package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for reporting a content post.
 *
 * @param reason      the reason for the report
 * @param description optional additional description (max 500 chars)
 * @author GYDI Development Team
 */
public record ReportContentRequest(
        @NotNull(message = "El motivo del reporte es requerido")
        ReportReason reason,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description
) {
}
