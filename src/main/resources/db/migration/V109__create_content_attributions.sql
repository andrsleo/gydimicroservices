-- V109: Create content_attributions table for UGC Social Commerce Phase 3
-- Links content posts to bookings for creator attribution and commission calculation

CREATE TABLE referrals.content_attributions (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES bookings.booking(id),
    content_post_id BIGINT REFERENCES content.content_posts(id),
    creator_id BIGINT NOT NULL REFERENCES users.users(id),
    referral_link_id BIGINT REFERENCES referrals.referral_links(id),
    attribution_type VARCHAR(20) NOT NULL CHECK (attribution_type IN ('DIRECT_VIEW', 'SHARED_LINK', 'CREATOR_LINK')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_attr_creator ON referrals.content_attributions(creator_id);
CREATE INDEX idx_content_attr_content ON referrals.content_attributions(content_post_id);
CREATE INDEX idx_content_attr_booking ON referrals.content_attributions(booking_id);
CREATE INDEX idx_content_attr_referral_link ON referrals.content_attributions(referral_link_id);
