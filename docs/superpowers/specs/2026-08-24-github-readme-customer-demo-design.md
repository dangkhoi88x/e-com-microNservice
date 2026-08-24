# GitHub README customer demo design

## Goal

Make the current customer-facing storefront easy to preview on GitHub by embedding a short animated GIF in the root README.

## Scope

- Start the React/Vite web application locally.
- Record a 12–18 second customer journey: storefront landing page, product/category browsing, product detail, then adding an item to the cart.
- Store the optimized GIF in `docs/demo/customer-storefront.gif`.
- Add a `## Demo giao diện khách hàng` section near the README introduction and embed the GIF using a repository-relative image link.

## Capture design

The capture is a desktop viewport showing only the application browser tab. It contains no credentials, terminal output, or unrelated desktop content. Actions are paced so the landing page and each state can be understood without narration. The run uses the existing local demo data/state exposed by the application; the capture does not create or mutate persistent business data.

## Technical approach

Use the existing Vite command to serve the frontend and browser automation to perform the route transitions. Record an MP4/WebM source if the browser recorder produces one, then create an optimized GIF constrained to a practical repository size. If backend dependencies prevent a live customer flow, retain the successfully rendered customer screens and document the limitation rather than fabricating UI state.

## README behavior

GitHub renders GIFs stored in the repository inline in Markdown, so contributors can see the demo without downloading a separate video asset or visiting an external host. The README includes concise alt text to preserve a useful description when animation cannot be viewed.

## Verification

1. Run `npm run build` in `web-app`.
2. Confirm the GIF exists, is non-empty, and remains reasonably small for Git history.
3. Confirm the README link resolves to the GIF from the repository root.
4. Review the GIF frames for the agreed customer journey and for accidental sensitive/desktop content.
