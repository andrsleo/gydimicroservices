-- Phase 4: Notifications bounded context
-- Migration: V110__create_notifications_schema.sql

CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT       NOT NULL REFERENCES users.users(id),
    type         VARCHAR(30)  NOT NULL
                   CHECK (type IN ('NEW_LIKE','NEW_FOLLOWER','BOOKING_FROM_CONTENT','CONTENT_MILESTONE')),
    title        VARCHAR(100) NOT NULL,
    body         VARCHAR(255),
    entity_id    BIGINT,
    entity_type  VARCHAR(30),
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    read_at      TIMESTAMP
);

CREATE INDEX idx_notif_recipient
    ON notifications.notifications(recipient_id, is_read, created_at DESC);
