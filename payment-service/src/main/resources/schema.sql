-- PostgreSQL partial unique index: an order may have historical FAILED or
-- CANCELLED payments, but it may never have two active PENDING payments.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_one_pending_per_order
    ON payments (order_id)
    WHERE status = 'PENDING';
