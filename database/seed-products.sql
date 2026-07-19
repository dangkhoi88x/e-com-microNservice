-- NovaShop product catalog seed for PostgreSQL.
-- Run against the product-service database (currently: postgres on port 5432).
-- Safe to run repeatedly: categories and products use stable slugs with ON CONFLICT.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO categories (id, name, description, slug)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'Điện thoại', 'Smartphone và phụ kiện công nghệ', 'dien-thoai'),
  ('10000000-0000-0000-0000-000000000002', 'Thời trang', 'Quần áo, giày dép và phụ kiện', 'thoi-trang'),
  ('10000000-0000-0000-0000-000000000003', 'Chăm sóc da', 'Mỹ phẩm và sản phẩm chăm sóc cá nhân', 'cham-soc-da'),
  ('10000000-0000-0000-0000-000000000004', 'Nhà cửa', 'Đồ gia dụng cho không gian sống', 'nha-cua')
ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

INSERT INTO products (id, name, description, price, quantity, seller_id, category_id, images, slug, status)
VALUES
  ('20000000-0000-0000-0000-000000000001', 'iPhone 15 Pro Max', 'Smartphone cao cấp với thiết kế titanium, camera Pro và hiệu năng mạnh mẽ.', 29990000, 40, 'seed-seller', '10000000-0000-0000-0000-000000000001',
   '[{"url":"https://images.unsplash.com/photo-1696446701796-da61225697cc?auto=format&fit=crop&w=900&q=80","isPrimary":true,"displayOrder":0}]'::jsonb, 'iphone-15-pro-max', 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000002', 'Giày Nova Runner', 'Giày chạy bộ nhẹ, êm và phù hợp cho hoạt động hằng ngày.', 1290000, 80, 'seed-seller', '10000000-0000-0000-0000-000000000002',
   '[{"url":"https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80","isPrimary":true,"displayOrder":0}]'::jsonb, 'giay-nova-runner', 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000003', 'Serum Torriden Dưỡng Ẩm', 'Serum cấp ẩm sâu, làm dịu và phục hồi hàng rào bảo vệ da.', 350000, 60, 'seed-seller', '10000000-0000-0000-0000-000000000003',
   '[{"url":"https://images.unsplash.com/photo-1612817288484-6f916006741a?auto=format&fit=crop&w=900&q=80","isPrimary":true,"displayOrder":0}]'::jsonb, 'serum-torriden-duong-am', 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000004', 'Máy lọc không khí Mini', 'Máy lọc không khí nhỏ gọn dành cho phòng ngủ và bàn làm việc.', 1890000, 25, 'seed-seller', '10000000-0000-0000-0000-000000000004',
   '[{"url":"https://images.unsplash.com/photo-1585771724684-38269d6639fd?auto=format&fit=crop&w=900&q=80","isPrimary":true,"displayOrder":0}]'::jsonb, 'may-loc-khong-khi-mini', 'ACTIVE')
ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, price = EXCLUDED.price, quantity = EXCLUDED.quantity, images = EXCLUDED.images, status = EXCLUDED.status;

-- Add a larger catalog for category/search/list-page testing.
INSERT INTO products (id, name, description, price, quantity, seller_id, category_id, images, slug, status)
SELECT
  gen_random_uuid()::text,
  'Sản phẩm mẫu Nova ' || n,
  'Sản phẩm dữ liệu mẫu phục vụ kiểm thử catalogue, tìm kiếm và phân trang.',
  99000 + n * 45000,
  15 + (n % 50),
  'seed-seller',
  CASE n % 4
    WHEN 0 THEN (SELECT id FROM categories WHERE slug = 'dien-thoai')
    WHEN 1 THEN (SELECT id FROM categories WHERE slug = 'thoi-trang')
    WHEN 2 THEN (SELECT id FROM categories WHERE slug = 'cham-soc-da')
    ELSE (SELECT id FROM categories WHERE slug = 'nha-cua')
  END,
  jsonb_build_array(jsonb_build_object('url', 'https://placehold.co/900x900/e7f2f8/3b82c4?text=Nova+' || n, 'isPrimary', true, 'displayOrder', 0)),
  'san-pham-mau-nova-' || n,
  'ACTIVE'
FROM generate_series(1, 24) AS n
ON CONFLICT (slug) DO NOTHING;

-- Product options for iPhone 15 Pro Max.
INSERT INTO product_options (id, product_id, name, display_name, display_type, display_order, required)
SELECT id, (SELECT id FROM products WHERE slug = 'iphone-15-pro-max'), name, display_name, display_type, display_order, required
FROM (VALUES
  ('30000000-0000-0000-0000-000000000001'::uuid, 'color', 'Màu sắc', 'COLOR_SWATCH', 0, true),
  ('30000000-0000-0000-0000-000000000002'::uuid, 'storage', 'Dung lượng', 'BUTTON', 1, true)
) AS seed(id, name, display_name, display_type, display_order, required)
ON CONFLICT (product_id, name) DO NOTHING;

INSERT INTO product_option_values (id, option_id, value, display_value, color_hex, display_order, active)
VALUES
  ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Natural Titanium', 'Titan tự nhiên', '#B7B1A6', 0, true),
  ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'Blue Titanium', 'Titan xanh', '#516B82', 1, true),
  ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', '256GB', '256GB', null, 0, true),
  ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', '512GB', '512GB', null, 1, true)
ON CONFLICT (option_id, value) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, attributes, price, quantity, image_url, status)
SELECT id, (SELECT id FROM products WHERE slug = 'iphone-15-pro-max'), sku, attributes, price, quantity, image_url, status
FROM (VALUES
  ('40000000-0000-0000-0000-000000000001'::uuid, 'IP15PM-NAT-256', '{"color":"Natural Titanium","storage":"256GB"}'::jsonb, 29990000::numeric, 10, null::text, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000002'::uuid, 'IP15PM-NAT-512', '{"color":"Natural Titanium","storage":"512GB"}'::jsonb, 34990000::numeric, 8, null::text, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000003'::uuid, 'IP15PM-BLU-256', '{"color":"Blue Titanium","storage":"256GB"}'::jsonb, 29990000::numeric, 12, null::text, 'ACTIVE')
) AS seed(id, sku, attributes, price, quantity, image_url, status)
ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, quantity = EXCLUDED.quantity, status = EXCLUDED.status;

-- Product options and variants for Nova Runner shoes.
INSERT INTO product_options (id, product_id, name, display_name, display_type, display_order, required)
SELECT id, (SELECT id FROM products WHERE slug = 'giay-nova-runner'), name, display_name, display_type, display_order, required
FROM (VALUES
  ('30000000-0000-0000-0000-000000000003'::uuid, 'color', 'Màu sắc', 'COLOR_SWATCH', 0, true),
  ('30000000-0000-0000-0000-000000000004'::uuid, 'size', 'Kích cỡ', 'BUTTON', 1, true)
) AS seed(id, name, display_name, display_type, display_order, required)
ON CONFLICT (product_id, name) DO NOTHING;

INSERT INTO product_option_values (id, option_id, value, display_value, color_hex, display_order, active)
VALUES
  ('31000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000003', 'Red', 'Đỏ', '#DF3C3C', 0, true),
  ('31000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000003', 'Black', 'Đen', '#1E293B', 1, true),
  ('31000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000004', '40', '40', null, 0, true),
  ('31000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000004', '41', '41', null, 1, true),
  ('31000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000004', '42', '42', null, 2, true)
ON CONFLICT (option_id, value) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, attributes, price, quantity, image_url, status)
SELECT id, (SELECT id FROM products WHERE slug = 'giay-nova-runner'), sku, attributes, price, quantity, image_url, status
FROM (VALUES
  ('40000000-0000-0000-0000-000000000004'::uuid, 'NOVA-RUN-RED-40', '{"color":"Red","size":"40"}'::jsonb, 1290000::numeric, 14, null::text, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000005'::uuid, 'NOVA-RUN-RED-41', '{"color":"Red","size":"41"}'::jsonb, 1290000::numeric, 16, null::text, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000006'::uuid, 'NOVA-RUN-BLK-42', '{"color":"Black","size":"42"}'::jsonb, 1290000::numeric, 18, null::text, 'ACTIVE')
) AS seed(id, sku, attributes, price, quantity, image_url, status)
ON CONFLICT (sku) DO UPDATE SET price = EXCLUDED.price, quantity = EXCLUDED.quantity, status = EXCLUDED.status;

-- Optional inventory seed. Run these lines against inventory-service database only,
-- and keep one inventory row per product with the current inventory schema.
-- INSERT INTO inventories (id, product_id, available_quantity, reserved_quantity, sold_quantity, created_at, updated_at)
-- VALUES ('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 40, 0, 0, NOW(), NOW())
-- ON CONFLICT (product_id) DO UPDATE SET available_quantity = EXCLUDED.available_quantity;
