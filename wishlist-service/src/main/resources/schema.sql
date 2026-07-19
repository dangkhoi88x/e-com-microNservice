-- A missing variant is stored as an empty string so PostgreSQL can enforce one
-- wishlist item per user/product/variant combination.
UPDATE wishlist_items SET variant_id = '' WHERE variant_id IS NULL;
ALTER TABLE wishlist_items ALTER COLUMN variant_id SET DEFAULT '';
ALTER TABLE wishlist_items ALTER COLUMN variant_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_user_product_variant
    ON wishlist_items (user_id, product_id, variant_id);
