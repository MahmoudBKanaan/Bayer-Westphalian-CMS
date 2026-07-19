# Appendix D - Deployment Evidence

Design and procedures exist for production Compose, Nginx HTTPS, environment/secrets, persistent
PostgreSQL and consent evidence, logs, health checks, backups, restore, smoke, rollback, monitoring
and incidents. Primary references are `docker-compose.prod.yml`, `docs/deployment`, and
`docs/operations/operations-guide.md`.

Current status is **BLOCKED for production release**. The retained item 738 report states that no
approved deployed production application was available. Deployment screenshots and runtime checks
must identify the exact commit, tag and environment and must not expose secrets.
