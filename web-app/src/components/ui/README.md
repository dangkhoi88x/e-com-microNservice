# UI design pattern

This project now supports a shadcn-style component layer next to the existing MUI screens.

## Rules

- New reusable primitives live in `src/components/ui`.
- Admin-specific composition components live in `src/components/admin`.
- Use `cn()` from `src/lib/utils.js` for class merging.
- Prefer shadcn primitives for new pages or gradual refactors.
- Do not mix MUI and shadcn inside one small component unless the component is bridging old UI.

## Import examples

```jsx
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader, Surface } from "@/components/admin";
```

## Current primitives

- `Button`
- `Card`
- `Input`
- `Badge`

## Admin compositions

- `PageHeader`
- `Surface`
- `StatCard`
