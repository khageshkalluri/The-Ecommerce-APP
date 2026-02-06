-- Enable UUID extension (needed for uuid_generate_v4)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Drop table if it already exists
DROP TABLE IF EXISTS products;

-- Create products table with UUID as primary key
CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          name VARCHAR(50) NOT NULL,
                          description TEXT,
                          price NUMERIC(10,2) NOT NULL,
                          quantity INT NOT NULL
);

-- Insert sample products (UUID auto-generated)
INSERT INTO products (name, description, price, quantity) VALUES
                                                              ('Wireless Keyboard', 'Compact Bluetooth keyboard with long battery life', 2499.00, 50),
                                                              ('Gaming Mouse', 'High precision optical mouse with RGB lighting', 1999.00, 100),
                                                              ('Laptop Stand', 'Adjustable aluminum stand for ergonomic comfort', 1499.00, 75),
                                                              ('Noise Cancelling Headphones', 'Over-ear headphones with active noise cancellation', 4999.00, 30);
