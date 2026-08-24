# GitHub README admin demo design

## Goal

Add an animated GIF that demonstrates the Admin workspace's read-only overview flow in the root README.

## Journey

1. Authenticate locally with the Admin account seeded by the project.
2. Show the Admin Dashboard overview.
3. Navigate to Analytics and show aggregate operational metrics.
4. Navigate to Products and use only non-mutating viewing or filtering controls.

The capture does not create, edit, approve, publish, hide, delete, or submit any resource. It excludes orders and payments because their records can contain customer or transaction data.

## Privacy and security

Authentication happens only in a temporary browser-local session. The login credentials never appear in the command line, README, GIF, source assets, or Git history. Review every exported frame and redact any visible personal contact data, private identifiers, or unrelated desktop content.

## Output and README

- File: `docs/demo/admin-workspace.gif`.
- Target: 10–12 seconds, 16:9, optimized GIF.
- README subsection: `### Admin Workspace`, after the existing customer and Seller Center GIF sections. The description states that it shows dashboard, analytics, and product monitoring only.

## Verification

1. Confirm the Admin Dashboard, Analytics, and Products pages render in the local project.
2. Verify no destructive or state-changing UI action was used.
3. Inspect representative frames for privacy, GIF duration, dimensions, and file size.
4. Run `npm run build` in `web-app`, then `git diff --check`.
