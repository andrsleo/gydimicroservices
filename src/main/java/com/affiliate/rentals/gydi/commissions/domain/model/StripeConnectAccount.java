package com.affiliate.rentals.gydi.commissions.domain.model;

import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidConnectAccountStateException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stripe Connect Account Aggregate Root.
 * <p>
 * Represents an affiliate's Stripe Connect account for receiving payouts.
 * Used for FASE 3: Affiliate onboarding with Stripe Connect.
 * </p>
 * <p>
 * Lifecycle: CREATED → ONBOARDING → ACTIVE (or DISABLED)
 * </p>
 * <p>
 * <strong>PURE DOMAIN MODEL</strong> - No framework dependencies (no JPA, Spring, etc.)
 * </p>
 */
public class StripeConnectAccount {

    // Identity
    private Long id;

    // Association
    private final Long userId;

    // Stripe Connect Account
    private final String stripeAccountId;
    private ConnectAccountType accountType;

    // Onboarding Status
    private boolean onboardingCompleted;
    private boolean detailsSubmitted;
    private boolean payoutsEnabled;
    private boolean chargesEnabled;

    // Country & Capabilities
    private String country;

    // Verification Status
    private String verificationStatus; // unverified, pending, verified, disabled

    // Stripe Platform Customer ID (cus_xxx) for charging host commissions
    private String stripePlatformCustomerId;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - use factory methods.
     */
    private StripeConnectAccount(
        Long id,
        Long userId,
        String stripeAccountId,
        ConnectAccountType accountType,
        boolean onboardingCompleted,
        boolean detailsSubmitted,
        boolean payoutsEnabled,
        boolean chargesEnabled,
        String country,
        String verificationStatus,
        String stripePlatformCustomerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.stripeAccountId = Objects.requireNonNull(stripeAccountId, "Stripe Account ID cannot be null");
        this.accountType = Objects.requireNonNull(accountType, "Account type cannot be null");
        this.onboardingCompleted = onboardingCompleted;
        this.detailsSubmitted = detailsSubmitted;
        this.payoutsEnabled = payoutsEnabled;
        this.chargesEnabled = chargesEnabled;
        this.country = country;
        this.verificationStatus = verificationStatus;
        this.stripePlatformCustomerId = stripePlatformCustomerId;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates a new Stripe Connect Account (CREATED state).
     */
    public static StripeConnectAccount create(
        Long userId,
        String stripeAccountId,
        ConnectAccountType accountType,
        String country
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new StripeConnectAccount(
            null, // ID assigned by persistence layer
            userId,
            stripeAccountId,
            accountType,
            false, // Onboarding not completed
            false, // Details not submitted
            false, // Payouts not enabled
            false, // Charges not enabled
            country,
            "unverified",
            null,  // No platform customer ID yet
            now,
            now
        );
    }

    /**
     * Reconstitution constructor for persistence layer.
     */
    public static StripeConnectAccount reconstitute(
        Long id,
        Long userId,
        String stripeAccountId,
        ConnectAccountType accountType,
        boolean onboardingCompleted,
        boolean detailsSubmitted,
        boolean payoutsEnabled,
        boolean chargesEnabled,
        String country,
        String verificationStatus,
        String stripePlatformCustomerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return new StripeConnectAccount(
            id, userId, stripeAccountId, accountType,
            onboardingCompleted, detailsSubmitted, payoutsEnabled, chargesEnabled,
            country, verificationStatus, stripePlatformCustomerId, createdAt, updatedAt
        );
    }

    // ========== BUSINESS LOGIC: STATE TRANSITIONS ==========

    /**
     * Updates account status from Stripe webhook (account.updated).
     *
     * @param detailsSubmitted Whether user completed onboarding form
     * @param payoutsEnabled Whether payouts are enabled
     * @param chargesEnabled Whether charges are enabled
     */
    public void updateFromStripeWebhook(
        boolean detailsSubmitted,
        boolean payoutsEnabled,
        boolean chargesEnabled
    ) {
        this.detailsSubmitted = detailsSubmitted;
        this.payoutsEnabled = payoutsEnabled;
        this.chargesEnabled = chargesEnabled;

        // Mark onboarding as completed if details submitted
        if (detailsSubmitted && !this.onboardingCompleted) {
            this.onboardingCompleted = true;
        }

        // Update verification status
        if (payoutsEnabled) {
            this.verificationStatus = "verified";
        } else if (detailsSubmitted) {
            this.verificationStatus = "pending";
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks account as disabled (due to Stripe suspension or manual action).
     */
    public void disable(String reason) {
        if (!canReceivePayouts()) {
            throw new InvalidConnectAccountStateException(
                "Account already disabled or not ready for payouts"
            );
        }

        this.payoutsEnabled = false;
        this.verificationStatus = "disabled";
        this.updatedAt = LocalDateTime.now();
    }

    // ========== QUERY METHODS ==========

    /**
     * Checks if account can receive payouts.
     */
    public boolean canReceivePayouts() {
        return onboardingCompleted && payoutsEnabled && verificationStatus.equals("verified");
    }

    /**
     * Checks if onboarding is complete.
     */
    public boolean isOnboardingComplete() {
        return onboardingCompleted && detailsSubmitted;
    }

    /**
     * Checks if account needs onboarding.
     */
    public boolean needsOnboarding() {
        return !onboardingCompleted || !detailsSubmitted;
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Cannot change Connect Account ID once set");
        }
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStripeAccountId() {
        return stripeAccountId;
    }

    public ConnectAccountType getAccountType() {
        return accountType;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public boolean isDetailsSubmitted() {
        return detailsSubmitted;
    }

    public boolean isPayoutsEnabled() {
        return payoutsEnabled;
    }

    public boolean isChargesEnabled() {
        return chargesEnabled;
    }

    public String getCountry() {
        return country;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public String getStripePlatformCustomerId() {
        return stripePlatformCustomerId;
    }

    /**
     * Sets the Stripe Platform Customer ID (cus_xxx) used for charging host commissions.
     */
    public void setPlatformCustomerId(String customerId) {
        this.stripePlatformCustomerId = customerId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StripeConnectAccount that = (StripeConnectAccount) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "StripeConnectAccount{id=%d, userId=%d, stripeAccountId=%s, onboardingCompleted=%s, payoutsEnabled=%s}",
            id, userId, stripeAccountId, onboardingCompleted, payoutsEnabled
        );
    }
}
