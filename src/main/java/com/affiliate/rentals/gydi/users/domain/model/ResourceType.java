package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration of resource types that are subject to subscription plan limits.
 *
 * <p>This enum identifies the types of resources that have creation/usage limits
 * based on a user's subscription plan. Each resource type has different limits
 * for FREE, PRO, and ELITE plans.</p>
 *
 * <p>Resource types include:</p>
 * <ul>
 *   <li>{@code PROPERTY} - Published properties (limited by plan)</li>
 *   <li>{@code REFERRAL} - Generated referral links (limited by plan)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ResourceType type = ResourceType.PROPERTY;
 * String description = type.getDescription();
 * String pluralName = type.getPluralName();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see SubscriptionPlan
 */
public enum ResourceType {
    /**
     * Property resource type.
     * Represents published vacation rental properties.
     * Limits: FREE=10, PRO=50, ELITE=unlimited
     */
    PROPERTY("Property", "Properties"),

    /**
     * Referral resource type.
     * Represents generated referral links.
     * Limits: FREE=50, PRO=200, ELITE=unlimited
     */
    REFERRAL("Referral", "Referrals");

    private final String singularName;
    private final String pluralName;

    /**
     * Constructs a ResourceType with the specified names.
     *
     * @param singularName the singular form of the resource name
     * @param pluralName the plural form of the resource name
     */
    ResourceType(String singularName, String pluralName) {
        this.singularName = singularName;
        this.pluralName = pluralName;
    }

    /**
     * Returns the singular name of this resource type.
     *
     * @return the singular name (e.g., "Property")
     */
    public String getSingularName() {
        return singularName;
    }

    /**
     * Returns the plural name of this resource type.
     *
     * @return the plural name (e.g., "Properties")
     */
    public String getPluralName() {
        return pluralName;
    }

    /**
     * Returns a human-readable description of this resource type.
     *
     * @return a formatted string describing the resource type
     */
    public String getDescription() {
        return switch (this) {
            case PROPERTY -> "Vacation rental properties that can be published on the platform";
            case REFERRAL -> "Referral links that can be generated for earning commissions";
        };
    }

    /**
     * Returns the resource name in lowercase (suitable for error messages).
     *
     * @return the lowercase singular name
     */
    public String getLowercaseName() {
        return singularName.toLowerCase();
    }

    /**
     * Returns the resource name in lowercase plural form.
     *
     * @return the lowercase plural name
     */
    public String getLowercasePluralName() {
        return pluralName.toLowerCase();
    }

    /**
     * Formats a count with the appropriate plural form.
     *
     * @param count the number of resources
     * @return a formatted string like "1 Property" or "5 Properties"
     */
    public String formatCount(int count) {
        return "%d %s".formatted(count, count == 1 ? singularName : pluralName);
    }

    @Override
    public String toString() {
        return singularName;
    }
}