# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build                      # compile + test
./gradlew test                       # tests only
./gradlew test --tests "ClassName"   # single test class
./gradlew test --tests "pkg.*Test"   # glob pattern
./gradlew bootRun                    # run locally (requires Postgres)
./gradlew bootJar                    # produce executable JAR
```

Start local Postgres first:
```bash
docker-compose up postgres -d
```

## Architecture

Single-module Spring Boot 4 app. Layered hexagonal layout:

| Package | Role |
|---|---|
| `api/` | REST controllers + WebSocket handlers |
| `domain/` | Core business logic, domain models |
| `torrent/` | Bencode parsing, `.torrent` metainfo |
| `tracker/` | HTTP/UDP tracker client |
| `peer/` | BitTorrent peer wire protocol |
| `storage/` | JPA repositories, persistence |
| `config/` | Spring bean configuration |

Root package: `io.github.denisbalako.bittorrent_client` (underscore, not hyphen — invalid in Java).

## Database

PostgreSQL 16. Flyway owns all schema changes — **never use `ddl-auto: create/update`**. Migrations live in `src/main/resources/db/migration/` (naming: `V{n}__{description}.sql`). JPA is set to `ddl-auto: validate`.

Local credentials (also used in docker-compose and CI):
- DB: `bittorrent`, user: `bittorrent`, pass: `bittorrent`, port: `5432`

## Environment Variables

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `DB_USER` | Postgres username |
| `DB_PASS` | Postgres password |

## CI

GitHub Actions (`.github/workflows/ci.yml`) — runs on push to `main`/`develop` and PRs to `main`. Spins up Postgres 16, runs `./gradlew build --no-daemon`. Uploads test reports on failure.

## Roadmap Context

- v0.1 scaffold is done (this branch)
- v0.2 = Bencode parser + torrent metainfo parsing (next)
- v0.3 = Tracker HTTP/UDP client
- v0.4 = Peer wire protocol
- v0.5 = REST API + Web UI
- v1.0 = Observability + production hardening

Most domain packages (`torrent/`, `tracker/`, `peer/`, `domain/`) are intentionally empty — scaffolded for upcoming work.
