package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for an admin reviewing a content report.
 *
 * @param status the new status to assign (REVIEWED, ACTIONED, DISMISSED)
 * @author GYDI Development Team
 */
public record ReviewReportRequest(
        @NotNull(message = "El estado del reporte es requerido")
        ReportStatus status
) {
}
