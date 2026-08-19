package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Value object representing user capabilities in the GYDI platform.
 *
 * <p>This immutable value object encapsulates the functional capabilities
 * that can be enabled or disabled for a user. These capabilities work in
 * conjunction with subscription plans to determine what actions a user can perform.</p>
 *
 * <p>Capabilities include:</p>
 * <ul>
 *   <li>{@code canPublish} - Ability to publish properties</li>
 *   <li>{@code canRefer} - Ability to generate referral links</li>
 *   <li>{@code canRent} - Ability to rent/book properties</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UserCapabilities capabilities = UserCapabilities.defaultCapabilities();
 * UserCapabilities disabledPublish = capabilities.withPublish(false);
 * boolean canRefer = capabilities.canRefer();
 * }</pre>
 *
 * @param canPublish whether the user can publish properties
 * @param canRefer whether the user can generate referral links
 * @param canRent whether the user can rent/book properties
 * @author GYDI Development Team
 */
public record UserCapabilities(
    boolean canPublish,
    boolean canRefer,
    boolean canRent
) {

    /**
     * Compact constructor that provides validation if needed.
     * Currently no validation is required as all fields are primitives.
     */
    public UserCapabilities {
        // No validation needed for boolean primitives
    }

    /**
     * Factory method to create UserCapabilities with all capabilities enabled.
     * This is the default state for newly registered users.
     *
     * @return a UserCapabilities instance with all capabilities set to {@code true}
     */
    public static UserCapabilities defaultCapabilities() {
        return new UserCapabilities(true, true, true);
    }

    /**
     * Factory method to create UserCapabilities with all capabilities disabled.
     * This can be used for suspended or restricted accounts.
     *
     * @return a UserCapabilities instance with all capabilities set to {@code false}
     */
    public static UserCapabilities allDisabled() {
        return new UserCapabilities(false, false, false);
    }

    /**
     * Factory method to create UserCapabilities with specific values.
     *
     * @param canPublish whether the user can publish properties
     * @param canRefer whether the user can generate referral links
     * @param canRent whether the user can rent/book properties
     * @return a new UserCapabilities instance
     */
    public static UserCapabilities of(boolean canPublish, boolean canRefer, boolean canRent) {
        return new UserCapabilities(canPublish, canRefer, canRent);
    }

    /**
     * Creates a new UserCapabilities instance with the publish capability modified.
     * This method maintains immutability by returning a new instance.
     *
     * @param enabled the new value for canPublish
     * @return a new UserCapabilities instance with updated publish capability
     */
    public UserCapabilities withPublish(boolean enabled) {
        return new UserCapabilities(enabled, canRefer, canRent);
    }

    /**
     * Creates a new UserCapabilities instance with the refer capability modified.
     * This method maintains immutability by returning a new instance.
     *
     * @param enabled the new value for canRefer
     * @return a new UserCapabilities instance with updated refer capability
     */
    public UserCapabilities withRefer(boolean enabled) {
        return new UserCapabilities(canPublish, enabled, canRent);
    }

    /**
     * Creates a new UserCapabilities instance with the rent capability modified.
     * This method maintains immutability by returning a new instance.
     *
     * @param enabled the new value for canRent
     * @return a new UserCapabilities instance with updated rent capability
     */
    public UserCapabilities withRent(boolean enabled) {
        return new UserCapabilities(canPublish, canRefer, enabled);
    }

    /**
     * Checks if all capabilities are enabled.
     *
     * @return {@code true} if all capabilities are enabled, {@code false} otherwise
     */
    public boolean areAllEnabled() {
        return canPublish && canRefer && canRent;
    }

    /**
     * Checks if all capabilities are disabled.
     *
     * @return {@code true} if all capabilities are disabled, {@code false} otherwise
     */
    public boolean areAllDisabled() {
        return !canPublish && !canRefer && !canRent;
    }

    /**
     * Checks if at least one capability is enabled.
     *
     * @return {@code true} if at least one capability is enabled, {@code false} otherwise
     */
    public boolean hasAnyEnabled() {
        return canPublish || canRefer || canRent;
    }

    /**
     * Returns a human-readable description of the enabled capabilities.
     *
     * @return a formatted string describing enabled capabilities
     */
    public String getEnabledCapabilitiesDescription() {
        if (areAllDisabled()) {
            return "No capabilities enabled";
        }
        if (areAllEnabled()) {
            return "All capabilities enabled";
        }

        StringBuilder sb = new StringBuilder("Enabled: ");
        boolean first = true;

        if (canPublish) {
            sb.append("Publish");
            first = false;
        }
        if (canRefer) {
            if (!first) sb.append(", ");
            sb.append("Refer");
            first = false;
        }
        if (canRent) {
            if (!first) sb.append(", ");
            sb.append("Rent");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "UserCapabilities[canPublish=%s, canRefer=%s, canRent=%s]"
            .formatted(canPublish, canRefer, canRent);
    }
}