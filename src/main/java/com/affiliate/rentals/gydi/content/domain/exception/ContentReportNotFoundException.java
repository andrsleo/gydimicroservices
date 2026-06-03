package com.affiliate.rentals.gydi.content.domain.exception;

/**
 * Exception thrown when a content report cannot be found.
 *
 * @author GYDI Development Team
 */
public final class ContentReportNotFoundException extends ContentDomainException {

    public ContentReportNotFoundException(Long reportId) {
        super("Reporte de contenido no encontrado con ID: " + reportId);
    }
}
