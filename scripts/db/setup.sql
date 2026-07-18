-- ============================================================================
-- DMG Movie Ticket Booking — Database Setup Script
-- ============================================================================
-- Run this script directly on your PostgreSQL server to create all tables.
-- Usage: psql -U <username> -d <database> -f scripts/db/setup.sql
-- ============================================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cities table
CREATE TABLE IF NOT EXISTS cities (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL UNIQUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Theaters table
CREATE TABLE IF NOT EXISTS theaters (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    location        VARCHAR(255)    NOT NULL,
    city_id         BIGINT          NOT NULL REFERENCES cities(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Screens table
CREATE TABLE IF NOT EXISTS screens (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    total_seats     INTEGER         NOT NULL,
    theater_id      BIGINT          NOT NULL REFERENCES theaters(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seats table
CREATE TABLE IF NOT EXISTS seats (
    id              BIGSERIAL       PRIMARY KEY,
    row_label       VARCHAR(10)     NOT NULL,
    seat_number     INTEGER         NOT NULL,
    seat_type       VARCHAR(20)     NOT NULL,
    screen_id       BIGINT          NOT NULL REFERENCES screens(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Movies table (standalone movie management)
-- Added in the movie management feature. If your DB was created before this
-- feature was added, run the CREATE TABLE below.
-- ============================================================================
CREATE TABLE IF NOT EXISTS movies (
    id               BIGSERIAL       PRIMARY KEY,
    title            VARCHAR(255)    NOT NULL,
    description      TEXT,
    genre            VARCHAR(100),
    duration_minutes INTEGER         NOT NULL,
    language         VARCHAR(50),
    release_date     DATE,
    poster_url       VARCHAR(500),
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Shows table (references movies via movie_id FK)
-- ============================================================================
CREATE TABLE IF NOT EXISTS shows (
    id              BIGSERIAL       PRIMARY KEY,
    movie_id        BIGINT          NOT NULL REFERENCES movies(id),
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,
    base_price      NUMERIC(10,2)   NOT NULL,
    screen_id       BIGINT          NOT NULL REFERENCES screens(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Pricing Tiers table
CREATE TABLE IF NOT EXISTS pricing_tiers (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL UNIQUE,
    seat_type       VARCHAR(20)     NOT NULL,
    weekday_price   NUMERIC(10,2)   NOT NULL,
    weekend_price   NUMERIC(10,2)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Show-Pricing Tier join table
CREATE TABLE IF NOT EXISTS show_pricing_tiers (
    id              BIGSERIAL       PRIMARY KEY,
    show_id         BIGINT          NOT NULL REFERENCES shows(id),
    pricing_tier_id BIGINT          NOT NULL REFERENCES pricing_tiers(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    show_id         BIGINT          NOT NULL REFERENCES shows(id),
    status          VARCHAR(20)     NOT NULL,
    total_amount    NUMERIC(10,2)   NOT NULL,
    hold_expires_at TIMESTAMP,
    confirmed_at    TIMESTAMP,
    cancelled_at    TIMESTAMP,
    refunded_at     TIMESTAMP,
    refund_amount   NUMERIC(10,2),
    discount_code_id BIGINT,
    discount_amount NUMERIC(10,2),
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Booking-Seats join table
CREATE TABLE IF NOT EXISTS booking_seats (
    id              BIGSERIAL       PRIMARY KEY,
    booking_id      BIGINT          NOT NULL REFERENCES bookings(id),
    seat_id         BIGINT          NOT NULL REFERENCES seats(id),
    price           NUMERIC(10,2)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Refund Policies table
CREATE TABLE IF NOT EXISTS refund_policies (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL UNIQUE,
    hours_before_show   INTEGER         NOT NULL,
    refund_percentage   INTEGER         NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Discount Codes table
CREATE TABLE IF NOT EXISTS discount_codes (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    discount_amount NUMERIC(10,2)   NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    used            BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMP,
    used_by_user_id BIGINT,
    expires_at      TIMESTAMP,
    description     VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Indexes for performance
-- ========================
CREATE INDEX IF NOT EXISTS idx_theaters_city_id ON theaters(city_id);
CREATE INDEX IF NOT EXISTS idx_screens_theater_id ON screens(theater_id);
CREATE INDEX IF NOT EXISTS idx_seats_screen_id ON seats(screen_id);
CREATE INDEX IF NOT EXISTS idx_movies_title ON movies(title);
CREATE INDEX IF NOT EXISTS idx_shows_screen_id ON shows(screen_id);
CREATE INDEX IF NOT EXISTS idx_shows_movie_id ON shows(movie_id);
CREATE INDEX IF NOT EXISTS idx_shows_start_time ON shows(start_time);
CREATE INDEX IF NOT EXISTS idx_show_pricing_tiers_show_id ON show_pricing_tiers(show_id);
CREATE INDEX IF NOT EXISTS idx_show_pricing_tiers_pricing_tier_id ON show_pricing_tiers(pricing_tier_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_show_id ON bookings(show_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_hold_expires_at ON bookings(hold_expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_seats_booking_id ON booking_seats(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_seats_seat_id ON booking_seats(seat_id);
CREATE INDEX IF NOT EXISTS idx_discount_codes_code ON discount_codes(code);
