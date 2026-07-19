-- NovaShop inventory seed for PostgreSQL.
-- Run after database/seed-products.sql, against inventory-service database:
--   postgres://postgres:postgres@localhost:5435/inventory_db
--
-- This file is idempotent and never overwrites a record that already exists.
-- Product rows store aggregate stock; variant rows are the stock used when an
-- order specifies a variantId.

INSERT INTO inventories (
    id, product_id, variant_id, available_quantity, reserved_quantity,
    sold_quantity, created_at, updated_at
)
VALUES
    -- Aggregate inventory for the four featured products.
    ('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', NULL, 30, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', NULL, 48, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', NULL, 60, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', NULL, 25, 0, 0, NOW(), NOW()),

    -- iPhone 15 Pro Max variants: 10 + 8 + 12 = 30.
    ('50000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 10, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 8, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 12, 0, 0, NOW(), NOW()),

    -- Nova Runner variants: 14 + 16 + 18 = 48.
    ('50000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000004', 14, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000005', 16, 0, 0, NOW(), NOW()),
    ('50000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000006', 18, 0, 0, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- The extended catalog uses stable IDs in seed-products.sql, so its inventory
-- can be seeded in a separate database without querying Product Service.
INSERT INTO inventories (
    id, product_id, variant_id, available_quantity, reserved_quantity,
    sold_quantity, created_at, updated_at
)
SELECT
    ('51000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid::text,
    ('21000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid::text,
    NULL,
    15 + (n % 50),
    0,
    0,
    NOW(),
    NOW()
FROM generate_series(1, 24) AS n
ON CONFLICT DO NOTHING;
