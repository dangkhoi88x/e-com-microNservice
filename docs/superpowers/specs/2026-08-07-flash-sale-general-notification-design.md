# Flash Sale General Notification Design

## Goal

Allow an authenticated shopper to persist a `Notify me` preference even when no Flash Deal is currently scheduled. The preference survives page reloads and results in a notification shortly before each future Flash Deal starts.

## API

- `POST /api/v1/flash-deals/notifications/general` creates an enabled general subscription for the authenticated user. The operation is idempotent.
- `GET /api/v1/flash-deals/notifications/general` returns whether the authenticated user has that subscription.

## Persistence and delivery

A new promotion-service entity stores one general subscription per user. When the existing scheduled task finds Flash Deals beginning within 15 minutes, it materializes every active general subscription into the existing per-deal notification subscription table. Its existing unique constraint prevents duplicate subscriptions for a user and deal. The existing Kafka publishing flow then sends each resulting notification once.

## Frontend

The shop page loads the general subscription state for authenticated users. The empty Flash Sale state calls the new POST endpoint, shows the confirmed state after success, and renders that state after reload. The existing per-deal subscription behavior is unchanged.

## Error handling and verification

Unauthenticated users are redirected to the login page. Backend operations derive the user ID from the JWT and are idempotent. Tests cover duplicate subscription requests, the general-status endpoint, materializing a general subscription into a future deal, and the page retaining the status after reload.
