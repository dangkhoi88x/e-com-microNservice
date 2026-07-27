-- A product can have one aggregate inventory row and multiple variant rows.
-- PostgreSQL treats NULL values as distinct in a normal unique index, so the
-- product aggregate and variant rows need separate partial indexes.
-- Hibernate does not reliably alter existing tables when optimistic locking is
-- introduced, therefore add the version columns explicitly for local databases.
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory_reservations ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_one_aggregate_per_product
    ON inventories (product_id)
    WHERE variant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_one_row_per_variant
    ON inventories (product_id, variant_id)
    WHERE variant_id IS NOT NULL;
