ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;

ALTER TABLE products
    ADD CONSTRAINT products_status_check
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'INACTIVE'));

-- Seller drafts can contain variants before the product is approved. Keep the
-- variant lifecycle aligned with the parent product lifecycle.
ALTER TABLE product_variants DROP CONSTRAINT IF EXISTS product_variants_status_check;

ALTER TABLE product_variants
    ADD CONSTRAINT product_variants_status_check
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'INACTIVE'));
