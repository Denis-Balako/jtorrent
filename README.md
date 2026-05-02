# BitTorrent Client

A modern BitTorrent client built with Java 25 and Spring Boot 3.

![CI](https://github.com/YOUR_NAME/bittorrent-client/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![License](https://img.shields.io/badge/license-MIT-blue)

## Tech stack

- **Java 25** — Virtual Threads (Project Loom) for concurrent peer connections
- **Spring Boot 4.0** — WebFlux, WebSocket, Actuator
- **PostgreSQL + Flyway** — persistent torrent session state
- **Docker Compose** — one-command local setup

## Running locally

```bash
docker-compose up postgres -d
./gradlew bootRun
```

Open `http://localhost:8080/actuator/health`

## Architecture

> Diagram coming in v0.5

## Roadmap

- [x] v0.1 — Project scaffold, CI/CD
- [ ] v0.2 — Bencode parser + .torrent metainfo
- [ ] v0.3 — Tracker HTTP/UDP client
- [ ] v0.4 — Peer wire protocol
- [ ] v0.5 — REST API + Web UI
- [ ] v1.0 — Observability + production hardening
