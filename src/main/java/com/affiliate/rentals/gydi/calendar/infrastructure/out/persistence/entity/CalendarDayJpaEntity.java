package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "property_calendar", schema = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarDayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalendarDayStatus status;

    @Column(name = "custom_price_amount", precision = 15, scale = 2)
    private BigDecimal customPriceAmount;

    @Column(name = "custom_price_currency", length = 10)
    private String customPriceCurrency;

    @Column(name = "block_source", length = 50)
    private String blockSource;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
