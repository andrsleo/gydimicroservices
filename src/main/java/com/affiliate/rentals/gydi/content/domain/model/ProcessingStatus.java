package com.affiliate.rentals.gydi.content.domain.model;

/**
 * Processing status of a media file (e.g. Cloudinary transcoding).
 */
public enum ProcessingStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
