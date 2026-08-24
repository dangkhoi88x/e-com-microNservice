# GitHub README seller demo design

## Goal

Add an animated GIF that shows the Seller Center's read-only product-management experience in the root README.

## Journey

1. Authenticate locally with the seller test account supplied by the repository owner.
2. Show the Seller Dashboard overview.
3. Navigate to the seller product list and show catalog, stock, and moderation statuses.

The capture does not create, update, hide, submit, or delete any product. It does not open seller orders or shop settings, which can reveal customer or contact data.

## Privacy and security

Credentials are used only through browser-local authentication and never appear in the command line, README, GIF, source assets, or Git history. Before export, inspect every captured frame and redact any visible personal contact information, private identifiers, or unrelated desktop content.

## Output and README

- File: `docs/demo/seller-center.gif`.
- Target: 10–12 seconds, 16:9, optimized GIF.
- README subsection: `### Seller Center`, after the existing customer GIF sections, with a repository-relative image link and concise description.

## Verification

1. Confirm the Seller Dashboard and product list were successfully rendered with the supplied test account.
2. Inspect representative frames for privacy and that no destructive action occurred.
3. Verify the GIF duration, dimensions, and file size.
4. Run `npm run build` in `web-app`, then `git diff --check`.
