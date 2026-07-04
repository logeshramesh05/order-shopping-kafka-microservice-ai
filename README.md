# Order–Shipping–ARES Platform: Full Technical Documentation

**Repo contents analyzed:** `order/`, `shipping/`, `ares-service/`, `docker-compose.yml`
**Stack:** Java 21+, Spring Boot 4.0.6, Apache Kafka, MySQL/TiDB Cloud, H2, Docker
**Author's framing:** Order + Shipping is the original Kafka-driven microservice pair; ARES ("Autonomous Resilience Engine") is a new third service layered on top for observability, chaos engineering, and AI-assisted incident analysis.

---

## 1. What this system actually does

Three independently deployable Spring Boot services, wired together with Kafka and Docker Compose:

| Service | Port | Role |
|---|---|---|
| `order-service` | 8081 | Accepts an order, persists it, publishes it to Kafka |
| `shipping-service` | 8082 | Consumes orders from Kafka, persists a shipping record, exposes an endpoint to mark an order shipped and publish that fact back to Kafka. Also hosts the ARES **Resilience Engine** (chaos experiments) |
| `ares-service` | 8083 | Standalone platform-intelligence service: polls the health of the other two services + Docker + Kafka, stores snapshots, and calls an LLM (Groq) to explain incidents in plain English |

### 1.1 End-to-end flow

```
Client
  │  POST /api/orders/produce  { customerName, totalCost, address }
  ▼
order-service ──save──▶ MySQL/TiDB "orders" table
  │
  │  produces JSON string
  ▼
Kafka topic: order_topic
  │
  │  @KafkaListener consumes
  ▼
shipping-service ──save──▶ MySQL/TiDB "shipping" table
  │
  │  (later) DELETE /api/shipping/{orderId}
  ▼
shipping-service ──delete row, produce Long orderId──▶ Kafka topic: shipped_order_topic
```

```
ares-service (independent loop, every 30s by default)
  ├─ DockerCliObservationAdapter   → `docker ps` on the host socket
  ├─ KafkaAdminObservationAdapter  → AdminClient.describeCluster()
  ├─ ActuatorHealthRestAdapter     → GET order-service:8081/actuator/health
  │                                   GET shipping-service:8082/actuator/health
  ├─ JvmSystemMetricsAdapter       → CPU / memory / disk / thread count of its own JVM
  └─ ObservationService            → merges all of the above into one ObservationSnapshot,
                                       computes an overall HealthState, persists it (H2/MySQL)

ares-service (on demand)
  POST /api/operations/intelligence/analyze { incidentId, prompt, logs, metrics, health }
       → GroqAdapter → Groq OpenAI-compatible chat completion → structured JSON
       → rootCause / severity / businessImpact / recommendation / confidence / summary

shipping-service (on demand, chaos engineering)
  POST /api/ares/resilience/run  { experimentType, affectedService, durationSeconds }
       → LocalDockerOperationsPort → shells out to `docker stop/restart <container>`
       → persists ResilienceExperimentRecord, returns outcome
```

---

## 2. Service-by-service breakdown

### 2.1 `order-service`

- `Order` — JPA entity: `id, customerName, totalCost, address, shipped`.
- `OrderController.saveAndProducerOrder()` — saves to DB, then calls `OrderProducer.produceOrder()`. **Single HTTP call does two side effects** (DB write + Kafka publish) with no transactional link between them (see §4.1).
- `OrderProducer` — serializes `Order` to JSON with Jackson and sends it as a raw `String` to `order_topic` via `KafkaTemplate<String, String>`.
- No DTO layer — the JPA entity itself is the request body, the response body, and the Kafka payload.

### 2.2 `shipping-service`

- `Shipping` — a **second, independent** JPA entity that duplicates `customerName/totalCost/address` and adds `shipping` (boolean) instead of `shipped`.
- `ShippingConsumer.consumeOrder()` — `@KafkaListener` on `order_topic`, deserializes the JSON string straight into a `Shipping` object (relies on field-name coincidence between `Order` and `Shipping`, not a shared contract — see §4.2), then saves it via `ShippingController.saveShipping()`.
- `ShippingController`:
  - `DELETE /api/shipping/{orderId}` — the "mark as shipped" action. It **deletes** the row from `shipping` (rather than flipping a status flag) and publishes the bare `Long orderId` to `shipped_order_topic`.
  - `GET /api/shipping` — lists everything still pending shipment (since shipped rows are deleted).
- `ShippedOrderIdProducer` — separate `KafkaTemplate<String, Long>` (different value-serializer from the order producer) publishing just the ID.
- **ARES Resilience Engine lives inside this service** (`com.example.kafka.shipping.ares.resilience` package): a self-contained hexagonal slice — `ResilienceEnginePort` / `ResilienceEngineService` / `DockerOperationsPort` / `LocalDockerOperationsPort` — that runs Docker-based chaos experiments (stop container, restart, simulate latency/packet-loss/Kafka-outage/DB-outage) against the platform and journals every run to `ares_resilience_experiments`.

### 2.3 `ares-service`

Structured as clean **ports-and-adapters (hexagonal) architecture**, in contrast to `order`/`shipping`'s flat controller-service-repository style:

- **`observation` bounded context** — read-only platform telemetry.
  - Domain: `ObservationSnapshot`, `ObservedComponent`, `ResourceSample`, `ComponentType`, `HealthState` — all Java `record`s, immutable.
  - Ports: `DockerObservationPort`, `KafkaObservationPort`, `ActuatorHealthPort`, `SystemMetricsPort`, `ObservationHistoryPort`.
  - Adapters implement each port against a real technology (Docker CLI, Kafka AdminClient, RestTemplate + Actuator, JVM `OperatingSystemMXBean`, JPA).
  - `ObservationService.collectSnapshot()` fans out to every port, merges results, derives one `HealthState` (`DOWN` > `DEGRADED` > `UNKNOWN` > `UP` priority), and persists via the history port. A `@Scheduled` job runs this automatically every `ares.observation.collection-interval-ms` (default 30s); the same logic is also reachable synchronously via `POST /api/operations/observations/collect`.
- **`intelligence` bounded context** — AI-assisted analysis, fully decoupled from `observation`.
  - `AIAnalysisPort` is provider-neutral; `GroqAdapter` is the only current implementation (OpenAI-compatible `/chat/completions` against Groq, model configurable, defaults to `openai/gpt-oss-120b`).
  - The adapter builds a plain-text prompt (incident id, free-text prompt, logs, metrics, health), asks for **JSON-only** output, strips markdown code fences defensively, and parses into an `AIAnalysisResult`. If the API key is missing, the network call fails, or parsing fails, it degrades to a structured "unavailable" result instead of throwing — the controller always gets a 200 with a meaningful payload.
- Shared: `GlobalExceptionHandler` (`@RestControllerAdvice`) gives every endpoint a consistent `ErrorResponse` shape; `ObservationProperties` / `IntelligenceProperties` are `@ConfigurationProperties`-bound and `@Validated`, so misconfiguration fails fast at startup rather than at request time.

---

## 3. Infrastructure & deployment

`docker-compose.yml` wires: Zookeeper → Kafka (Confluent 7.4.1, dual listener for in-network + host access) → `order-service` → `shipping-service` → `ares-service` (depends on both). Notable choices:

- **Different persistence per service.** `order`/`shipping` point at a **shared external TiDB Cloud (MySQL-compatible) cluster**, two separate schemas (`orders`, `shipping`). `ares-service` defaults to an **in-memory H2** database (`MODE=MySQL`), overridable via `ARES_SPRING_DATASOURCE_URL`.
- **`ares-service` mounts the host Docker socket** (`/var/run/docker.sock:/var/run/docker.sock`) so its `DockerCliObservationAdapter` can run `docker ps` against the *host's* Docker daemon from inside a container. The chaos-engineering `LocalDockerOperationsPort` in `shipping-service` needs the same capability but the compose file doesn't mount the socket into `shipping-service` — meaning `docker stop/restart` calls from inside that container will currently fail unless it's run outside Docker or the socket is added there too (see §4.5).
- Kafka bootstrap servers are `kafka:29092` inside the compose network vs `localhost:9092` for local dev outside Docker — handled correctly via environment variable overrides in each `application.properties`.

---

## 4. Architectural tradeoffs — the "why" and the cost

This is the part that matters most for a resume/portfolio writeup or an interview: every one of these is a defensible decision *for a learning/demo project*, but each has a specific, nameable cost at production scale.

### 4.1 No transactional outbox / no distributed transaction between DB write and Kafka publish
**Decision:** `OrderController` calls `orderRepository.save()` then `orderProducer.produceOrder()` sequentially, in the same method, with no compensating logic.
**Tradeoff:** Simple, fast to build, easy to reason about in the happy path.
**Cost:** If the process crashes or Kafka is unreachable *after* the DB commit but before/during the `send()`, the order is saved but never reaches `shipping-service` — a silently lost order with no retry, no dead-letter, no outbox table. The inverse (Kafka succeeds, DB write later rolled back — not applicable here since save happens first, but generally) is the classic **dual-write problem**. Production fix: transactional outbox pattern, or Debezium CDC on the `orders` table instead of an application-level producer call.

### 4.2 Implicit schema coupling between `Order` and `Shipping` instead of a shared contract
**Decision:** `order-service` publishes `Order` serialized to JSON; `shipping-service` deserializes the same bytes directly into its own, separately-defined `Shipping` entity, relying on matching field names (`customerName`, `totalCost`, `address`) and silently dropping/defaulting the rest (`id` → new autoincrement `Id` in `shipping`, `shipped` boolean not mapped to `shipping` boolean — they're different fields entirely).
**Tradeoff:** Zero shared library, zero versioning ceremony, each service owns its own model — good for team/service autonomy.
**Cost:** No compile-time or schema-registry-enforced contract. A field rename in `order-service` (e.g. `customerName` → `customerFullName`) breaks `shipping-service` at runtime with a silent `null`, not a build failure. This is the textbook argument for a shared Avro/Protobuf schema + Confluent Schema Registry, or at minimum a shared DTO module, once more than one team touches these services.

### 4.3 Delete-as-completion instead of a status field
**Decision:** "Shipped" is modeled by **deleting** the row from the `shipping` table (`DELETE /api/shipping/{orderId}`) rather than setting `shipping = true`.
**Tradeoff:** Trivial query for "what's still pending" (`SELECT * FROM shipping` — no `WHERE` clause needed), and it's the fewest lines of code to demonstrate the producer→consumer→producer round trip.
**Cost:** Destroys the audit trail. There is no way to query "orders shipped last week" from this table ever again — that history now only exists as a Kafka message on `shipped_order_topic` (and only for as long as that topic's retention holds, or a consumer group has committed past it). Any downstream reporting, refund/dispute handling, or reconciliation job has nowhere to look. A `status` enum column plus a `shipped_at` timestamp costs one migration and avoids this permanently.

### 4.4 Two different `KafkaTemplate` value types across three topics, both using `String`/raw serializers instead of a schema
**Decision:** `order_topic` carries hand-serialized JSON `String`; `shipped_order_topic` carries a raw `Long`. Consumer side uses `spring.json.trusted.packages=*` for the wildcard trust (a known Spring Kafka footgun if this were ever pointed at an untrusted Kafka cluster, since it would deserialize arbitrary types).
**Tradeoff:** No Avro/Protobuf toolchain to set up; anyone can `kafkacat`/console-consume and read the payload as plain text — great for debugging a college/portfolio project.
**Cost:** No compatibility checking, no schema evolution safety net, and `trusted.packages=*` is flagged by most security scanners as a deserialization risk (even though the current payload is just a `String`, the setting itself is broad). Fine here because both producer and consumer are first-party code the author controls; would need tightening (`spring.json.trusted.packages=com.example.kafka.*` at minimum) before treating this as production-representative.

### 4.5 Hardcoded database credentials with real values as Spring property *defaults*
**Decision:** Both `order/src/main/resources/application.properties` and `shipping/.../application.properties` hardcode the **actual** TiDB Cloud username (`2AaAz4LnhRj3Eeq.root`) and password (`DHc5zd5PZUTXSj8G`) as the fallback value inside `${SPRING_DATASOURCE_PASSWORD:DHc5zd5PZUTXSj8G}` — meaning they ship to source control even though the *pattern* (env-var override) looks correctly externalized.
**Tradeoff:** Convenient for local `mvn spring-boot:run` without needing a `.env` file every time — this is almost certainly why it happened (fast iteration during a hackathon/internship-style build).
**Cost:** This is a genuine, live secret leak once the repo is pushed anywhere public or shared — exactly the security risk you (Logesh) already flagged. The `${VAR:default}` syntax is only safe when the default is a *placeholder*, never a real credential. **Fix, in order of effort:**
  1. Immediate: rotate the TiDB password now that it's been committed anywhere, treat the old one as burned.
  2. Change defaults to empty/placeholder strings so the app *fails loudly* if the env var isn't set, instead of silently falling back to a real (or now-revoked) credential.
  3. Move actual secrets into `.env` (already `git-ignore`d per the compose setup — `env_file: .env` is used for the containers) or a proper secrets manager, never into `application.properties`.
  4. Add a pre-commit hook or GitHub secret-scanning to prevent recurrence.

  ARES's own `application.properties` gets this right, by contrast — its default (`sa` / empty password against in-memory H2) is genuinely harmless because H2 is ephemeral, and the Groq key has an **empty** default (`${GROQ_API_KEY:}`), which is exactly the correct pattern.

### 4.6 Hexagonal architecture in `ares-service`, flat layering in `order`/`shipping`
**Decision:** The two original services use a conventional `Controller → Service(optional) → Repository` shape with entities doing double duty as DTOs. `ares-service` introduces explicit domain records, ports, and adapters, and keeps persistence entities (`ObservationMetricRecord`) separate from domain models (`ObservationSnapshot`).
**Tradeoff:** The hexagonal style in ARES makes it trivial to swap `GroqAdapter` for an OpenAI/Anthropic adapter later (just implement `AIAnalysisPort`), or to unit-test `ObservationService` with fake ports with zero Spring context — evidenced by the adapter-level unit tests present in `ares-service/src/test`. It costs more boilerplate (a port + an adapter + a DTO + a domain record for one concept) for a project this size.
**Cost/inconsistency:** The stack is now **architecturally inconsistent across services** — a reviewer (or a future contributor) has to learn two different conventions in one codebase. That's a fair tradeoff if ARES is deliberately the "showcase good architecture" module of the portfolio, but it's worth stating explicitly rather than leaving it implicit, since otherwise it reads as inconsistency rather than intent.

### 4.7 Observation Engine: polling/pull model, not event-driven
**Decision:** ARES's health picture is built by **actively polling** (Docker CLI, Kafka AdminClient, Actuator HTTP) every 30 seconds by default, rather than the other services pushing health events or metrics.
**Tradeoff:** Zero changes required to `order-service`/`shipping-service` beyond having Actuator on the classpath (which they don't currently expose — see §5) — ARES can observe *any* Spring Boot service without that service knowing ARES exists. Simple to reason about, easy to demo on a laptop.
**Cost:** 30-second blind spots between polls; a service that crashes and restarts inside one interval is invisible. Polling N services also means N sequential/parallel HTTP calls plus a `docker ps` shell-out plus a Kafka admin round trip every cycle — fine at 2 services, doesn't scale past a handful without either parallelizing the fan-out (it currently isn't — `observeServiceHealth()` iterates the map synchronously) or moving to push-based metrics (Micrometer → Prometheus scrape, which the `pom.xml` already pulls in via `micrometer-registry-prometheus` but isn't wired up yet for cross-service polling).

### 4.8 Graceful-degradation-over-exceptions in the Intelligence Engine
**Decision:** `GroqAdapter.analyze()` never throws out of its public method — missing API key, network failure, and unparsable AI output all become a valid `AIAnalysisResult` with `severity = UNKNOWN` and a human-readable `summary` explaining what went wrong.
**Tradeoff:** The `/analyze` endpoint is always a 200 with a body a UI can render directly, no special-casing 4xx/5xx GROQ failures on the client. This is genuinely a *good* production pattern for an "AI as a feature, not the critical path" use case.
**Cost:** Callers must inspect `severity`/`confidence` to know whether the answer is *real* — an unmonitored caller could silently trust a `confidence: 0.0, rootCause: "AI provider unavailable"` result as if it were a real diagnosis. Worth pairing with a `provider != "unavailable"`-style explicit status field if this ever becomes a UI a real on-call engineer stares at during an incident.

### 4.9 Chaos-engineering blast radius: real `docker stop/restart` on shared infrastructure, gated by nothing but the request body
**Decision:** `ResilienceController` (in `shipping-service`) accepts a POST with an arbitrary `affectedService` string and, for `STOP_CONTAINER`/`RESTART_SERVICE` experiment types, shells out to `docker stop <affectedService>` / `docker restart <affectedService>` with **no allow-list, no auth, no confirmation step**. `INJECT_LATENCY`/`SIMULATE_PACKET_LOSS`/`SIMULATE_KAFKA_FAILURE`/`SIMULATE_DATABASE_FAILURE` are currently **simulated in name only** — they return a canned string and don't actually touch `tc`/`netem`/toxiproxy/anything.
**Tradeoff:** This is honest, useful scaffolding — the port/adapter split (`DockerOperationsPort`) means swapping in a real latency-injection adapter (e.g. Toxiproxy, Pumba, or `tc netem` via another CLI adapter) later doesn't touch the controller or the persistence layer at all.
**Cost, if this ever runs anywhere but a personal sandbox:** `docker stop kafka` or `docker stop <anything on the host Docker daemon>` is possible from this endpoint today, since the string is passed straight through with only case-normalization, no allow-list of "experiments only ever target these containers." That's fine on `localhost`; it would need an allow-list + auth + maybe a "confirm" token before being safe to expose even on an internal network.

### 4.10 No API layer / no gateway / no auth anywhere in the three services
**Decision:** Every REST endpoint across all three services (`/api/orders`, `/api/shipping`, `/api/ares/resilience`, `/api/operations/*`) is unauthenticated and directly reachable on its mapped port.
**Tradeoff:** Removes an entire category of setup (API gateway, JWT/OAuth2 resource server config, CORS) from a project whose goal is demonstrating Kafka + observability + AI patterns, not auth.
**Cost:** Not remotely deployable as-is without an API gateway or Spring Security layer in front — worth stating as an explicit "not yet done, by design, for scope reasons" in any writeup rather than letting a reviewer assume it was missed.

---

## 5. Gaps worth naming (not necessarily fixing before your resume/interview, but worth knowing)

1. **No transactional outbox** for order → Kafka (see 4.1).
2. **No schema registry / Avro / Protobuf** — raw JSON strings on the wire (see 4.4).
3. **`order-service`/`shipping-service` don't expose Actuator** in their `pom.xml`s (only `ares-service` has `spring-boot-starter-actuator`), yet `ares-service`'s `ActuatorHealthRestAdapter` depends on `GET .../actuator/health` on those two services — this will 404/fail until Actuator is added to `order`/`shipping`'s dependencies. Worth double-checking against the actual runtime behavior described in memory (hardcoded credentials were already flagged as a risk — this would be the next thing a reviewer notices).
4. **Hardcoded live DB credentials** in two `application.properties` files (see 4.5) — highest-priority fix.
5. **Docker socket only mounted for `ares-service`**, not `shipping-service`, even though the Resilience Engine (which needs it) lives in `shipping-service` (see §3, 4.9).
6. **No allow-list/auth on chaos experiments** (see 4.9).
7. **Sequential, unparallelized health polling** in `ActuatorHealthRestAdapter.observeServiceHealth()` (see 4.7) — a `Map.forEach` doing blocking HTTP calls one at a time.
8. **No dead-letter topic / retry policy** configured on the `shipping-service` Kafka consumer — a poison-pill message on `order_topic` will loop per Spring Kafka's default error-handling behavior rather than being quarantined.

---

## 6. If you were narrating this in an interview

A clean way to frame this project's story, given the tradeoffs above:

- **Order + Shipping** demonstrates core event-driven microservice fundamentals: Kafka producer/consumer, per-service database ownership, REST APIs — deliberately kept simple to isolate and showcase the messaging pattern.
- **ARES** is the "level-up" module: same platform, but built with ports-and-adapters so that Docker, Kafka, Actuator, and an LLM provider are all swappable implementation details behind stable interfaces — and it adds three production-adjacent capabilities most personal projects skip: **continuous health observation, incident AI-analysis, and chaos engineering.**
- The known gaps (hardcoded creds, no transactional outbox, no schema registry, no auth) are the natural "what I'd do differently / what's next" answer — they show you can name the tradeoff, not just that a mistake happened. That's a stronger interview answer than pretending the project is production-ready.

---

## 7. Suggested next increments, roughly in priority order

1. Rotate the TiDB credentials; replace hardcoded defaults with empty placeholders.
2. Add `spring-boot-starter-actuator` to `order`/`shipping` `pom.xml`s so ARES's health polling actually works end-to-end.
3. Add a `status` enum (`PENDING`, `SHIPPED`, `CANCELLED`) to `shipping` instead of delete-as-completion.
4. Introduce a shared schema (even a simple shared DTO module, or Avro + Schema Registry if you want the resume line) between `order` and `shipping`.
5. Parallelize `ActuatorHealthRestAdapter` health checks (`CompletableFuture`/virtual threads — Java 21 gives you both cheaply).
6. Add an allow-list of experiment-eligible container names to the Resilience Engine, and mount the Docker socket into `shipping-service` if chaos experiments are meant to run from there.
7. Wire Micrometer → Prometheus → Grafana for the metrics ARES already collects, instead of only exposing them via REST/H2.
