# GitHub README search and deals demos design

## Goal

Add two short, separate animated GIFs to the root README so GitHub visitors can preview product discovery and current promotional content without running the application.

## GIF 1: Search and filtering

- File: `docs/demo/customer-search-filter.gif`.
- Journey: open search, enter `iPhone`, select the Điện thoại category, apply the in-stock filter and price sort, then show the updated results.
- The capture demonstrates client navigation and live search-index data only. It neither signs in nor changes server data.

## GIF 2: Deals and promotions

- File: `docs/demo/customer-deals.gif`.
- Journey: open the Ưu đãi page, show the active promotion cards when available, then show the Hot deal product list and category navigation.
- The capture never claims a promotion. If no active promotion exists, the rendered empty state is shown truthfully instead of fabricated promotion data.

## README placement

Add a `### Tìm kiếm và lọc sản phẩm` subsection and a `### Ưu đãi và Hot Deal` subsection after the existing customer-storefront GIF. Each uses repository-relative Markdown image links, which GitHub renders inline.

## Capture and quality

Each GIF is 10–12 seconds, 16:9, and optimized for a small repository footprint. Frames come exclusively from the local application using the running services and current demo data. Review every final frame for visible credentials, personal information, unrelated desktop content, and accurate feature claims.

## Verification

1. Run `npm run build` in `web-app`.
2. Verify each GIF exists, has a non-zero duration, and is small enough for normal Git history.
3. Confirm all three GIF paths in README resolve from the repository root.
4. Inspect representative frames of both new GIFs and run `git diff --check`.
