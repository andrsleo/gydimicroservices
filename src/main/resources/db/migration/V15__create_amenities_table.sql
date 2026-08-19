-- V16: Create amenities table and property_amenities junction table
-- This migration creates a normalized amenities system

-- Create amenities table
CREATE TABLE IF NOT EXISTS properties.amenities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon VARCHAR(50),
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create property_amenities junction table (many-to-many)
CREATE TABLE IF NOT EXISTS properties.property_amenities (
    property_id BIGINT NOT NULL REFERENCES properties.properties(id) ON DELETE CASCADE,
    amenity_id INTEGER NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (property_id, amenity_id),
    CONSTRAINT fk_property_amenities_property
        FOREIGN KEY (property_id)
        REFERENCES properties.properties(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_property_amenities_amenity
        FOREIGN KEY (amenity_id)
        REFERENCES properties.amenities(id)
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_property_amenities_property_id ON properties.property_amenities(property_id);
CREATE INDEX idx_property_amenities_amenity_id ON properties.property_amenities(amenity_id);
CREATE INDEX idx_amenities_category ON properties.amenities(category);
CREATE INDEX idx_amenities_name ON properties.amenities(name);

-- Insert default amenities
INSERT INTO properties.amenities (name, description, category) VALUES
-- Essential Amenities
('WiFi', 'Wireless internet connection', 'Essential'),
('Air Conditioning', 'Climate control - cooling', 'Essential'),
('Heating', 'Climate control - heating', 'Essential'),
('Hot Water', 'Hot water available', 'Essential'),
('Kitchen', 'Full kitchen with appliances', 'Essential'),
('Washer', 'Washing machine', 'Essential'),
('Dryer', 'Clothes dryer', 'Essential'),
('Free Parking', 'Free parking on premises', 'Essential'),

-- Entertainment
('TV', 'Television', 'Entertainment'),
('Cable TV', 'Cable television service', 'Entertainment'),
('Streaming Service', 'Netflix, Prime Video, etc.', 'Entertainment'),
('Game Console', 'PlayStation, Xbox, etc.', 'Entertainment'),
('Board Games', 'Board games available', 'Entertainment'),

-- Kitchen & Dining
('Coffee Maker', 'Coffee making facilities', 'Kitchen'),
('Dishwasher', 'Automatic dishwasher', 'Kitchen'),
('Microwave', 'Microwave oven', 'Kitchen'),
('Refrigerator', 'Full-size refrigerator', 'Kitchen'),
('Oven', 'Cooking oven', 'Kitchen'),
('Stove', 'Cooking stove/range', 'Kitchen'),
('Toaster', 'Bread toaster', 'Kitchen'),
('Blender', 'Food blender', 'Kitchen'),
('Dining Table', 'Dining table and chairs', 'Kitchen'),

-- Outdoor
('Pool', 'Swimming pool', 'Outdoor'),
('Hot Tub', 'Hot tub/jacuzzi', 'Outdoor'),
('BBQ Grill', 'Barbecue grill', 'Outdoor'),
('Garden', 'Garden or yard', 'Outdoor'),
('Balcony', 'Private balcony', 'Outdoor'),
('Terrace', 'Terrace or patio', 'Outdoor'),
('Beach Access', 'Direct beach access', 'Outdoor'),
('Lake Access', 'Access to lake', 'Outdoor'),
('Mountain View', 'Mountain view', 'Outdoor'),
('Ocean View', 'Ocean view', 'Outdoor'),
('City View', 'City view', 'Outdoor'),

-- Bathroom
('Hair Dryer', 'Hair dryer', 'Bathroom'),
('Shampoo', 'Shampoo provided', 'Bathroom'),
('Conditioner', 'Conditioner provided', 'Bathroom'),
('Body Soap', 'Body soap/shower gel', 'Bathroom'),
('Bathtub', 'Bathtub available', 'Bathroom'),
('Shower', 'Shower available', 'Bathroom'),

-- Bedroom & Laundry
('Iron', 'Iron and ironing board', 'Bedroom'),
('Hangers', 'Clothes hangers', 'Bedroom'),
('Bed Linens', 'Bed linens provided', 'Bedroom'),
('Extra Pillows', 'Extra pillows and blankets', 'Bedroom'),
('Blackout Curtains', 'Room darkening shades', 'Bedroom'),
('Clothing Storage', 'Closet or dresser', 'Bedroom'),

-- Family Features
('Crib', 'Baby crib', 'Family'),
('High Chair', 'High chair for children', 'Family'),
('Baby Bath', 'Baby bath', 'Family'),
('Children''s Books', 'Children''s books and toys', 'Family'),
('Baby Monitor', 'Baby monitor', 'Family'),

-- Work & Productivity
('Workspace', 'Dedicated workspace', 'Work'),
('Desk', 'Work desk', 'Work'),
('Office Chair', 'Ergonomic office chair', 'Work'),
('Monitor', 'Computer monitor', 'Work'),
('Printer', 'Printer available', 'Work'),

-- Heating & Cooling
('Fireplace', 'Indoor fireplace', 'Climate'),
('Ceiling Fan', 'Ceiling fans', 'Climate'),
('Portable Fan', 'Portable fans', 'Climate'),

-- Gym & Wellness
('Gym', 'Fitness equipment', 'Wellness'),
('Yoga Mat', 'Yoga mat provided', 'Wellness'),
('Sauna', 'Sauna', 'Wellness'),
('Massage Chair', 'Massage chair', 'Wellness'),

-- Safety & Security
('Security System', 'Security alarm system', 'Safety'),
('First Aid Kit', 'First aid kit', 'Safety'),
('Fire Extinguisher', 'Fire extinguisher', 'Safety'),
('Smoke Detector', 'Smoke detector', 'Safety'),
('Carbon Monoxide Detector', 'CO detector', 'Safety'),
('Lock on Bedroom Door', 'Bedroom door lock', 'Safety'),
('Safe', 'Safe for valuables', 'Safety'),

-- Building Amenities
('Elevator', 'Elevator in building', 'Building'),
('Doorman', 'Doorman or concierge', 'Building'),
('Gym in Building', 'Building gym access', 'Building'),
('Pool in Building', 'Building pool access', 'Building'),

-- Accessibility
('Wheelchair Accessible', 'Wheelchair accessible', 'Accessibility'),
('Step-Free Access', 'Step-free guest entrance', 'Accessibility'),
('Wide Doorways', 'Wide doorways', 'Accessibility'),
('Accessible Bathroom', 'Accessible bathroom', 'Accessibility'),

-- Policies
('Pet Friendly', 'Pets allowed', 'Policy'),
('Smoking Allowed', 'Smoking permitted', 'Policy'),
('Events Allowed', 'Events/parties allowed', 'Policy'),
('Long Term Stays', 'Long term stays allowed', 'Policy'),

-- Additional Services
('Cleaning Service', 'Cleaning service available', 'Service'),
('Breakfast Included', 'Breakfast included', 'Service'),
('Airport Shuttle', 'Airport shuttle service', 'Service'),
('Luggage Storage', 'Luggage storage', 'Service')
ON CONFLICT (name) DO NOTHING;

-- Add comment to tables
COMMENT ON TABLE properties.amenities IS 'Master table of available property amenities';
COMMENT ON TABLE properties.property_amenities IS 'Junction table linking properties to their amenities';
COMMENT ON COLUMN properties.amenities.category IS 'Category grouping: Essential, Entertainment, Kitchen, Outdoor, Bathroom, Bedroom, Family, Work, Climate, Wellness, Safety, Building, Accessibility, Policy, Service';
