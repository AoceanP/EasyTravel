# Keycloak realm

`realm-easytravel.json` is imported when the Keycloak container first starts.

- **Roles:** `customer` (book and manage own trips) and `ops` (the Ops room).
- **Demo accounts:** these are public, demo-only test accounts for this local stack, shown on the website's Sign in page:

  | User | Password | Roles |
  |---|---|---|
  | `alice` | `alice-demo` | customer |
  | `ops-admin` | `ops-demo` | customer, ops |

- **Client `web-ui`:** a confidential OpenID Connect client using the authorization code flow. Its secret is **not** in this file: `${EASYTRAVEL_OIDC_SECRET}` is filled from the environment at import, and comes from your local `.env` (created by `scripts/setup`).
- **Roles in tokens:** a mapper copies realm roles into a `groups` claim, which Jakarta Security reads as the caller's groups.
- `sslRequired` is `none` because this demo runs over plain HTTP on your own machine. Any real deployment must use HTTPS and `external` or `all`.
- **Login theme:** `themes/easytravel/` extends Keycloak's `keycloak.v2` theme and only changes its look (light, the website's colours and font, amber button). The realm selects it with `"loginTheme": "easytravel"`.
