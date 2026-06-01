-- V114: Create collaborations schema
-- Part of: Creator Collaboration Marketplace (feature 001)

CREATE SCHEMA IF NOT EXISTS collaborations;

-- ============================================================
-- TABLE: collaborations.pitches
-- Central aggregate: one active pitch per creator per property
-- ============================================================
CREATE TABLE collaborations.pitches (
    id                   BIGSERIAL    PRIMARY KEY,
    property_id          BIGINT       NOT NULL REFERENCES properties.properties(id),
    host_id              BIGINT       NOT NULL REFERENCES users.users(id),
    creator_id           BIGINT       NOT NULL REFERENCES users.users(id),
    introduction         TEXT         NOT NULL CHECK (char_length(introduction) <= 1000),
    portfolio_url        TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                      CHECK (status IN ('PENDING','COUNTERED','ACCEPTED','DECLINED','EXPIRED','CANCELLED','COMPLETED')),
    counter_offer_rounds SMALLINT     NOT NULL DEFAULT 0 CHECK (counter_offer_rounds <= 3),
    preferred_check_in   DATE         NOT NULL,
    preferred_check_out  DATE         NOT NULL,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    declined_reason      TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Prevent duplicate active pitches from same creator to same property
CREATE UNIQUE INDEX pitches_active_creator_property_idx
    ON collaborations.pitches (creator_id, property_id)
    WHERE status IN ('PENDING', 'COUNTERED', 'ACCEPTED');

CREATE INDEX pitches_host_id_status_idx    ON collaborations.pitches (host_id, status);
CREATE INDEX pitches_creator_id_idx        ON collaborations.pitches (creator_id);
CREATE INDEX pitches_expires_at_idx        ON collaborations.pitches (expires_at)
    WHERE status IN ('PENDING', 'COUNTERED');

-- ============================================================
-- TABLE: collaborations.pitch_deliverables
-- Deliverables proposed in a pitch
-- ============================================================
CREATE TABLE collaborations.pitch_deliverables (
    id               BIGSERIAL   PRIMARY KEY,
    pitch_id         BIGINT      NOT NULL REFERENCES collaborations.pitches(id) ON DELETE CASCADE,
    deliverable_type VARCHAR(20) NOT NULL CHECK (deliverable_type IN ('reel','photo','story','video','blog_post','tiktok')),
    quantity         SMALLINT    NOT NULL CHECK (quantity > 0),
    notes            TEXT
);

CREATE INDEX pitch_deliverables_pitch_id_idx ON collaborations.pitch_deliverables (pitch_id);

-- ============================================================
-- TABLE: collaborations.pitch_compensation
-- Single compensation structure per pitch
-- ============================================================
CREATE TABLE collaborations.pitch_compensation (
    id                       BIGSERIAL      PRIMARY KEY,
    pitch_id                 BIGINT         NOT NULL UNIQUE REFERENCES collaborations.pitches(id) ON DELETE CASCADE,
    compensation_type        VARCHAR(25)    NOT NULL CHECK (compensation_type IN ('free_stay','cash','hybrid','affiliate','experience_exchange')),
    nights                   SMALLINT,
    amount                   NUMERIC(10,2),
    currency                 CHAR(3),
    booking_commission_pct   NUMERIC(5,2)   CHECK (booking_commission_pct BETWEEN 0 AND 100),
    experience_items         TEXT[]
);

-- ============================================================
-- TABLE: collaborations.counter_offers
-- Each counter-offer round (max 3 per pitch)
-- ============================================================
CREATE TABLE collaborations.counter_offers (
    id                     BIGSERIAL      PRIMARY KEY,
    pitch_id               BIGINT         NOT NULL REFERENCES collaborations.pitches(id) ON DELETE CASCADE,
    round_number           SMALLINT       NOT NULL CHECK (round_number BETWEEN 1 AND 3),
    offered_by             VARCHAR(10)    NOT NULL CHECK (offered_by IN ('HOST', 'CREATOR')),
    message                TEXT,
    compensation_type      VARCHAR(25)    CHECK (compensation_type IN ('free_stay','cash','hybrid','affiliate','experience_exchange')),
    nights                 SMALLINT,
    amount                 NUMERIC(10,2),
    currency               CHAR(3),
    booking_commission_pct NUMERIC(5,2)   CHECK (booking_commission_pct BETWEEN 0 AND 100),
    experience_items       TEXT[],
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (pitch_id, round_number)
);

CREATE INDEX counter_offers_pitch_id_idx ON collaborations.counter_offers (pitch_id);

-- ============================================================
-- TABLE: collaborations.counter_offer_deliverables
-- Deliverable modifications proposed in a counter-offer
-- ============================================================
CREATE TABLE collaborations.counter_offer_deliverables (
    id                BIGSERIAL   PRIMARY KEY,
    counter_offer_id  BIGINT      NOT NULL REFERENCES collaborations.counter_offers(id) ON DELETE CASCADE,
    deliverable_type  VARCHAR(20) NOT NULL CHECK (deliverable_type IN ('reel','photo','story','video','blog_post','tiktok')),
    quantity          SMALLINT    NOT NULL CHECK (quantity > 0),
    notes             TEXT
);

CREATE INDEX counter_offer_deliverables_offer_id_idx ON collaborations.counter_offer_deliverables (counter_offer_id);

-- ============================================================
-- TABLE: collaborations.agreements
-- Generated on pitch acceptance; digital collaboration contract
-- ============================================================
CREATE TABLE collaborations.agreements (
    id                   BIGSERIAL   PRIMARY KEY,
    pitch_id             BIGINT      NOT NULL UNIQUE REFERENCES collaborations.pitches(id),
    property_id          BIGINT      NOT NULL,
    host_id              BIGINT      NOT NULL,
    creator_id           BIGINT      NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (status IN ('ACTIVE','IN_PROGRESS','DELIVERED','COMPLETED','CANCELLED')),
    check_in_date        DATE        NOT NULL,
    check_out_date       DATE        NOT NULL,
    delivery_deadline    DATE        NOT NULL,
    posting_deadline     DATE        NOT NULL,
    cancellation_policy  TEXT        NOT NULL,
    cancelled_by         VARCHAR(10) CHECK (cancelled_by IN ('HOST', 'CREATOR')),
    cancelled_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX agreements_host_id_idx    ON collaborations.agreements (host_id);
CREATE INDEX agreements_creator_id_idx ON collaborations.agreements (creator_id);
CREATE INDEX agreements_status_idx     ON collaborations.agreements (status);

-- ============================================================
-- TABLE: collaborations.agreement_deliverables
-- Agreed deliverables (copied from pitch/counter-offer on acceptance)
-- ============================================================
CREATE TABLE collaborations.agreement_deliverables (
    id                BIGSERIAL   PRIMARY KEY,
    agreement_id      BIGINT      NOT NULL REFERENCES collaborations.agreements(id) ON DELETE CASCADE,
    deliverable_type  VARCHAR(20) NOT NULL CHECK (deliverable_type IN ('reel','photo','story','video','blog_post','tiktok')),
    quantity          SMALLINT    NOT NULL CHECK (quantity > 0),
    status            VARCHAR(25) NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','SUBMITTED','APPROVED','REVISION_REQUESTED')),
    revision_feedback TEXT
);

CREATE INDEX agreement_deliverables_agreement_id_idx ON collaborations.agreement_deliverables (agreement_id);

-- ============================================================
-- TABLE: collaborations.delivery_assets
-- Files uploaded by creator against an agreement deliverable
-- ============================================================
CREATE TABLE collaborations.delivery_assets (
    id                          BIGSERIAL    PRIMARY KEY,
    agreement_id                BIGINT       NOT NULL REFERENCES collaborations.agreements(id),
    agreement_deliverable_id    BIGINT       NOT NULL REFERENCES collaborations.agreement_deliverables(id),
    file_url                    TEXT         NOT NULL,
    file_type                   VARCHAR(10)  NOT NULL CHECK (file_type IN ('mp4','jpg','png','zip')),
    file_size_bytes             BIGINT       NOT NULL CHECK (file_size_bytes > 0),
    review_status               VARCHAR(25)  NOT NULL DEFAULT 'PENDING'
                                             CHECK (review_status IN ('PENDING','APPROVED','REVISION_REQUESTED')),
    uploaded_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX delivery_assets_deliverable_id_idx ON collaborations.delivery_assets (agreement_deliverable_id);

-- ============================================================
-- TABLE: collaborations.content_rights
-- Usage rights granted per agreement (multiple rows per agreement)
-- ============================================================
CREATE TABLE collaborations.content_rights (
    id               BIGSERIAL   PRIMARY KEY,
    agreement_id     BIGINT      NOT NULL REFERENCES collaborations.agreements(id) ON DELETE CASCADE,
    right_type       VARCHAR(20) NOT NULL CHECK (right_type IN ('organic_social','paid_ads','website','email','unlimited','time_limited')),
    duration_months  SMALLINT    CHECK (duration_months IN (3, 6, 12, 24))
);

CREATE INDEX content_rights_agreement_id_idx ON collaborations.content_rights (agreement_id);
