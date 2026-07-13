# TLS Certificate Mounts

Item **722** configures HTTPS termination in the production Nginx service. Certificate material is
not stored in this directory or committed to Git.

Set these deployment environment variables to readable host files:

- `TLS_CERTIFICATE_PATH`: full certificate chain in PEM format.
- `TLS_PRIVATE_KEY_PATH`: matching private key in PEM format.

Compose mounts them read-only as `/etc/nginx/tls/fullchain.pem` and
`/etc/nginx/tls/privkey.pem`. Restrict private-key filesystem permissions to the deployment
operator and Docker service account. Renew certificates through the host certificate manager, then
reload or recreate the reverse-proxy container.

Never place real `.pem` or `.key` files in the repository. Both extensions are ignored globally.
