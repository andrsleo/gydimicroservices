CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL -- Ej: 'OWNER', 'GUEST', 'ADMIN'
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE properties (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location TEXT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    bedrooms INT,
    bathrooms INT,
    beds INT,
    capacity INT,
    principal_image TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE property_owners (
    property_id BIGINT REFERENCES properties(id),
    user_id BIGINT REFERENCES users(id),
    PRIMARY KEY (property_id, user_id)
);

CREATE TABLE amenities_catalog (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE property_amenities (
    property_id BIGINT REFERENCES properties(id),
    amenity_id BIGINT REFERENCES amenities_catalog(id),
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE property_media (
    id BIGINT PRIMARY KEY,
    property_id BIGINT REFERENCES properties(id),
    media_url TEXT NOT NULL,
    media_type VARCHAR(20) CHECK (media_type IN ('IMAGE', 'VIDEO')),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE booking_status (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY,
    property_id BIGINT REFERENCES properties(id),
    user_id BIGINT REFERENCES users(id), -- quien reserva
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status_id BIGINT REFERENCES booking_status(id),
    created_at TIMESTAMP DEFAULT NOW()
);

