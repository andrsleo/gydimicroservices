package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(schema = "collaborations", name = "delivery_assets")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agreement_id", nullable = false)
    private Long agreementId;

    @Column(name = "agreement_deliverable_id", nullable = false)
    private Long agreementDeliverableId;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "review_status", nullable = false, length = 25)
    private String reviewStatus;

    @Column(name = "uploaded_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = OffsetDateTime.now();
        if (reviewStatus == null) reviewStatus = "PENDING";
    }
}
