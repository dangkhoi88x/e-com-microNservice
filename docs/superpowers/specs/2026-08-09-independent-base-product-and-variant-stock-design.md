# Independent base-product and variant stock

## Goal

Keep the base product and every product variant as separate purchasable SKUs. Adding a variant must not make the base product appear out of stock.

## Inventory model

- The base product uses the inventory row with `variantId = null` and `product.quantity`.
- A variant uses its inventory row with its own `variantId` and `variant.quantity`.
- Inventory events for a variant update only that variant. Inventory events for the base product update only the base product.

## Storefront behavior

The product detail page will show a selectable **Sản phẩm gốc** choice whenever a product has variants. It is selected by default.

- With **Sản phẩm gốc** selected, the UI displays `product.quantity` and adds to cart with `variantId = null`.
- With a variant selected, the UI displays that variant's quantity and adds to cart with its `variantId`.
- The UI must never substitute `0` merely because variants exist.

## Error handling and verification

The existing cart validation remains the source of truth for stock at checkout. The frontend will disable purchase only for the currently selected SKU when its quantity is zero. Verify that a product with base stock and one variant can add either SKU to cart independently and that their quantities remain unchanged by selecting the other SKU.
