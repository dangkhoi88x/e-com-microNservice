-- PostgreSQL partial unique index: an order may have historical FAILED or
-- CANCELLED payments, but it may never have two active PENDING payments.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_one_pending_per_order
    ON payments (order_id)
    WHERE status = 'PENDING';

-- Hibernate does not update an existing enum check constraint when a new Java
-- enum value is added. Keep the database constraint aligned with
-- PaymentMethod so STRIPE payments can be persisted on upgraded databases.
ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_method_check;

ALTER TABLE payments
    ADD CONSTRAINT payments_method_check
    CHECK (method IN ('COD', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'STRIPE'));
