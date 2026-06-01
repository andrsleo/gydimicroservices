package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.ContentRightType;

import java.util.Objects;
import java.util.Set;

/**
 * Value object representing a usage right granted in an agreement.
 * For TIME_LIMITED type, durationMonths must be one of: 3, 6, 12, 24.
 * Pure Java — no framework annotations.
 */
public final class ContentRight {

    private static final Set<Integer> VALID_DURATIONS = Set.of(3, 6, 12, 24);

    private final Long id;
    private final ContentRightType type;
    private final Integer durationMonths;

    private ContentRight(Long id, ContentRightType type, Integer durationMonths) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (type == ContentRightType.TIME_LIMITED) {
            Objects.requireNonNull(durationMonths, "durationMonths required for TIME_LIMITED");
            if (!VALID_DURATIONS.contains(durationMonths))
                throw new IllegalArgumentException("durationMonths must be one of: 3, 6, 12, 24");
        }
        this.durationMonths = durationMonths;
    }

    public static ContentRight of(ContentRightType type) {
        if (type == ContentRightType.TIME_LIMITED)
            throw new IllegalArgumentException("Use timeLimited(int months) for TIME_LIMITED");
        return new ContentRight(null, type, null);
    }

    public static ContentRight timeLimited(int durationMonths) {
        return new ContentRight(null, ContentRightType.TIME_LIMITED, durationMonths);
    }

    public static ContentRight reconstitute(Long id, ContentRightType type, Integer durationMonths) {
        return new ContentRight(id, type, durationMonths);
    }

    public Long id() { return id; }
    public ContentRightType type() { return type; }
    public Integer durationMonths() { return durationMonths; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentRight that)) return false;
        return type == that.type && Objects.equals(durationMonths, that.durationMonths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, durationMonths);
    }
}
