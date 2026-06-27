# Khoi Micro API Bruno Collection

## How to use

1. Open Bruno.
2. Choose `Open Collection`.
3. Select this folder: `bruno/khoi-micro-api`.
4. Select the `Local` environment.
5. Run `Auth/Login` first, or paste a valid JWT into `accessToken`.
6. Run the `Category` requests from `01` to `09`.

## Local variables

- `productBaseUrl`: product-service through API gateway, default `http://localhost:9191/product`.
- `identityBaseUrl`: identity-service URL, default `http://localhost:8080/identity/api`.
- `accessToken`: set automatically by `Auth/Login`.
- `categoryId`: set automatically by `01 Create Category`.
- `categorySlug`: set automatically by create/update category requests.

The API gateway routes `/product/**` to product-service and strips the `/product` prefix.
