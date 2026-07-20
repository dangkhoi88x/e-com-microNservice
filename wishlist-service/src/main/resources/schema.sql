-- A missing variant is stored as an empty string so PostgreSQL can enforce one
-- wishlist item per user/product/variant combination.
UPDATE wishlist_items SET variant_id = '' WHERE variant_id IS NULL;
ALTER TABLE wishlist_items ALTER COLUMN variant_id SET DEFAULT '';
ALTER TABLE wishlist_items ALTER COLUMN variant_id SET NOT NULL;

-- Older frontend versions could submit the same product more than once. Keep
-- the first saved item for each user/product/variant before enforcing the key.
WITH duplicate_items AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, product_id, variant_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM wishlist_items
)
DELETE FROM wishlist_items
WHERE id IN (SELECT id FROM duplicate_items WHERE row_number > 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_user_product_variant
    ON wishlist_items (user_id, product_id, variant_id);
