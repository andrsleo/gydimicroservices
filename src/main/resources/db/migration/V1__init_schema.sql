-- ============================================
-- Script de Base de Datos - Sistema de Propiedades
-- ============================================

-- Crear extensión para UUID (asegura IDs únicos)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ====================
-- Tabla: Owners
-- ====================
CREATE TABLE owners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(20)
);

-- ====================
-- Tabla: Properties
-- ====================
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(200) NOT NULL,
    price_per_night DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'COP',
    bedrooms INT DEFAULT 0,
    bathrooms INT DEFAULT 0,
    beds INT DEFAULT 0,
    capacity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- ====================
-- Tabla: Amenities
-- ====================
CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- ====================
-- Tabla: Media Catalogs
-- ====================
CREATE TABLE media_catalogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    media_url TEXT NOT NULL,
    media_type VARCHAR(20) CHECK (media_type IN ('image','video')),
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- ====================
-- Tabla: Users
-- ====================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ====================
-- Tabla: Bookings
-- ====================
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    user_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) CHECK (status IN ('pending','confirmed','cancelled')) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ====================
-- Índices para optimizar consultas
-- ====================
CREATE INDEX idx_properties_location ON properties(location);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_property ON bookings(property_id);
CREATE INDEX idx_media_property ON media_catalogs(property_id);
CREATE INDEX idx_amenities_property ON amenities(property_id);

-- ============================================
-- Inserciones de Ejemplo
-- ============================================

-- Owners
INSERT INTO owners (name, email, phone)
VALUES
('Juan Pérez', 'juan.perez@example.com', '+57 3001234567'),
('María Gómez', 'maria.gomez@example.com', '+57 3109876543'),
('Carlos Rodríguez', 'carlos.rodriguez@example.com', '+57 3154567890'),
('Ana Martínez', 'ana.martinez@example.com', '+57 3201239876'),
('Luis Herrera', 'luis.herrera@example.com', '+57 3016543210');

-- Properties
INSERT INTO properties (owner_id, title, description, location, price_per_night, currency, bedrooms, bathrooms, beds, capacity)
VALUES
((SELECT id FROM owners LIMIT 1 OFFSET 0), 'Casa en la playa', 'Hermosa casa frente al mar', 'Cartagena, Colombia', 350000, 'COP', 3, 2, 4, 6),
((SELECT id FROM owners LIMIT 1 OFFSET 1), 'Apartamento moderno', 'Apartamento en el centro de la ciudad', 'Bogotá, Colombia', 200000, 'COP', 2, 1, 2, 4),
((SELECT id FROM owners LIMIT 1 OFFSET 2), 'Cabaña en la montaña', 'Cabaña rústica con chimenea', 'Medellín, Colombia', 180000, 'COP', 2, 1, 3, 5),
((SELECT id FROM owners LIMIT 1 OFFSET 3), 'Villa con piscina', 'Villa de lujo con piscina privada', 'Santa Marta, Colombia', 500000, 'COP', 4, 3, 5, 8),
((SELECT id FROM owners LIMIT 1 OFFSET 4), 'Loft minimalista', 'Loft con diseño moderno', 'Cali, Colombia', 250000, 'COP', 1, 1, 1, 2);

-- Amenities
INSERT INTO amenities (property_id, name)
VALUES
((SELECT id FROM properties LIMIT 1 OFFSET 0), 'WiFi'),
((SELECT id FROM properties LIMIT 1 OFFSET 0), 'Aire acondicionado'),
((SELECT id FROM properties LIMIT 1 OFFSET 1), 'Parqueadero'),
((SELECT id FROM properties LIMIT 1 OFFSET 2), 'Chimenea'),
((SELECT id FROM properties LIMIT 1 OFFSET 3), 'Piscina');

-- Media Catalogs
INSERT INTO media_catalogs (property_id, media_url, media_type)
VALUES
((SELECT id FROM properties LIMIT 1 OFFSET 0), 'https://example.com/playa1.jpg', 'image'),
((SELECT id FROM properties LIMIT 1 OFFSET 1), 'https://example.com/apto1.jpg', 'image'),
((SELECT id FROM properties LIMIT 1 OFFSET 2), 'https://example.com/cabana1.jpg', 'image'),
((SELECT id FROM properties LIMIT 1 OFFSET 3), 'https://example.com/villa1.jpg', 'image'),
((SELECT id FROM properties LIMIT 1 OFFSET 4), 'https://example.com/loft1.jpg', 'image');

-- Users
INSERT INTO users (name, email, password_hash)
VALUES
('Pedro López', 'pedro.lopez@example.com', 'hash1'),
('Laura Torres', 'laura.torres@example.com', 'hash2'),
('Andrés Muñoz', 'andres.munoz@example.com', 'hash3'),
('Camila Díaz', 'camila.diaz@example.com', 'hash4'),
('Felipe Ramírez', 'felipe.ramirez@example.com', 'hash5');

-- Bookings
INSERT INTO bookings (property_id, user_id, start_date, end_date, total_price, status)
VALUES
((SELECT id FROM properties LIMIT 1 OFFSET 0), (SELECT id FROM users LIMIT 1 OFFSET 0), '2025-09-20', '2025-09-25', 1750000, 'confirmed'),
((SELECT id FROM properties LIMIT 1 OFFSET 1), (SELECT id FROM users LIMIT 1 OFFSET 1), '2025-10-01', '2025-10-05', 800000, 'pending'),
((SELECT id FROM properties LIMIT 1 OFFSET 2), (SELECT id FROM users LIMIT 1 OFFSET 2), '2025-11-10', '2025-11-15', 900000, 'confirmed'),
((SELECT id FROM properties LIMIT 1 OFFSET 3), (SELECT id FROM users LIMIT 1 OFFSET 3), '2025-12-20', '2025-12-27', 3500000, 'cancelled'),
((SELECT id FROM properties LIMIT 1 OFFSET 4), (SELECT id FROM users LIMIT 1 OFFSET 4), '2026-01-05', '2026-01-07', 500000, 'confirmed');

ADD COLUMN principal_image TEXT NOT NULL DEFAULT 'https://example.com/default-property.jpg';

