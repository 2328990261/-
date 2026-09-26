## Context

See `proposal.md` for the motivation. The backend already has the Redis starter dependency and a placeholder thread-pool class, but Redis connection properties are bound under the wrong Spring Boot namespace and the thread-pool bean is not registered or lifecycle-managed. Recommendation generation performs many sequential database reads, tag heat is recomputed in memory on request threads, and EPUB import parses every file serially on the request thread.

The implementation must preserve the existing recommendation algorithm and API contracts. MySQL remains the source of truth. Verification-code storage is explicitly outside this change.

## Goals / Non-Goals

**Goals:**

- Use the correct Spring Boot 3.2 Redis connection namespace and centralize cache operations.
- Cache expensive user-scoped and public read paths with explicit TTLs.
- Invalidate user-scoped caches through a user recommendation version.
- Degrade supported reads to MySQL when Redis is disabled or unavailable.
- Separate recommendation, upload, and statistics workloads into bounded Spring-managed executors.
- Preserve the existing synchronous EPUB upload response while processing files concurrently.
- Add enough logging to distinguish cache hits, misses, failures, executor activity, and rejected work.

**Non-Goals:**

- No verification-code storage changes.
- No recommendation-algorithm changes.
- No frontend task center or asynchronous upload task API.
- No chapter-text caching.
- No Java 21 virtual-thread rollout.
- No message queue, distributed job scheduler, or multi-instance coordination beyond Redis cache access.
- No Redis-backed read-count buffering; that remains future work.

## Decisions

### 1. Use `StringRedisTemplate` with JSON values

Create a single cache support component around `StringRedisTemplate`. Serialize values as JSON with the application's `ObjectMapper` and use explicit string keys with a `novel:` prefix.

**Rationale:** String keys are inspectable in Redis, JSON avoids JDK serialization compatibility and security issues, and a central component makes TTL, logging, feature switching, and fallback behavior consistent.

**Alternative considered:** Spring Cache annotations with `@Cacheable` and `@CacheEvict`. This was not chosen as the primary approach because several cache boundaries are inside larger service methods and need user-version composition, conditional fallback, and debug metadata that are clearer with explicit operations.

### 2. Bind Redis with `spring.data.redis`

Move connection settings under `spring.data.redis`, with host, port, password, command timeout, and connect timeout configurable through environment variables.

**Rationale:** This is the supported namespace for Spring Boot 3.2. The current `spring.cache.redis` block configures the cache manager, not the Redis connection.

### 3. Use a user recommendation version for personalized caches

Store a per-user version key:

```text
novel:rec:version:{userId}
```

Use it in effective-tag-weight and recommendation-result keys:

```text
novel:rec:weights:{userId}:{version}
novel:rec:result:{userId}:{sortType}:{version}
```

Increment the version after collection, reading-history, finished-novel, comment, dislike, preference-tag, or recommendation-profile mutations. Look up the version before loading personalized data; if Redis fails, execute the original calculation.

**Rationale:** Versioning avoids scanning and deleting user keys and gives a deterministic boundary between old and new user behavior. Shared versioning keeps recommendation results and weights coherent.

**Alternative considered:** Deleting each affected key directly. This requires key enumeration or maintaining key sets and is more error-prone than changing a version segment.

### 4. Use TTL-based public caches with targeted invalidation

Cache carousel data, category lists, book details, and recommendation configuration under `novel:public:*` and `novel:config:*` with short TTLs. Add invalidation calls in administrative mutation paths for the corresponding data.

**Rationale:** These reads are public and read-heavy. TTL bounds memory and provides a safety net even if an invalidation is missed; explicit invalidation reduces stale data after administrative edits.

**Alternative considered:** TTL-only invalidation. It is simpler but can show recently changed data later than expected and does not satisfy the administrative-refresh requirement.

### 5. Cache tag heat as a computed aggregate

Move the current site-tag-heat result from local fields to Redis. On cache expiry, submit recomputation to the statistics executor, write the refreshed aggregate, and serve a stale value while refresh is in progress when one exists. If no value exists, perform a synchronous fallback computation.

**Rationale:** The computation scans multiple behavior tables and is global rather than user-specific. Redis makes the aggregate shareable, while the statistics executor keeps the scan off request threads.

### 6. Replace the placeholder pool with three `ThreadPoolTaskExecutor` beans

Create `recommendExecutor`, `uploadExecutor`, and `statsExecutor` as Spring-managed `ThreadPoolTaskExecutor` beans in a `@Configuration` class. Initial parameters:

| Executor | Core | Max | Queue | Keep-alive | Rejection |
|---|---:|---:|---:|---:|---|
| `recommendExecutor` | 6 | 10 | 200 | 60s | Abort, then fallback |
| `uploadExecutor` | 2 | 4 | 20 | 60s | Abort with user-facing failure |
| `statsExecutor` | 1 | 2 | 10 | 60s | Caller-runs |

All pools use workload-specific thread-name prefixes, bounded queues, and graceful-shutdown settings. Remove or replace the existing `ThreadPool` class.

**Rationale:** Returning `ThreadPoolTaskExecutor` preserves Spring lifecycle management and graceful shutdown. Separate pools prevent a long EPUB parse or tag-heat scan from consuming recommendation workers.

**Alternative considered:** A single general pool or the common `ForkJoinPool`. Both allow one workload to starve others and provide less control over rejection and shutdown.

### 7. Use explicit executor submission rather than `@Async`

Inject the named executor and use `CompletableFuture.supplyAsync(..., executor)` for recommendation reads and other bounded tasks. Do not add `@EnableAsync` or `@Async` in this change.

**Rationale:** Explicit submission makes the execution boundary visible, avoids proxy self-invocation issues, and avoids accidental transaction or request-context assumptions.

### 8. Parallelize only independent recommendation reads

Submit independent data loads, such as configuration, profile, preferences, blacklist, behavior summaries, and book metadata, to `recommendExecutor`. Aggregate required futures with a bounded overall timeout. If a parallel path fails or times out, log it and use the existing sequential path where feasible.

**Rationale:** The current recommendation method has a long call chain, but not all inputs are independent. Bounded parallelism can reduce latency without overwhelming MySQL.

### 9. Keep EPUB upload synchronous while processing files concurrently

Before dispatching work, copy each uploaded file to a temporary file or byte buffer. Submit parsing and volume creation for accepted files to `uploadExecutor`, collect results, and return the existing success count after all submitted work finishes. Track per-file failures independently.

**Rationale:** This preserves the current frontend and API contract while using the pool. Copying input before dispatch avoids relying on a `MultipartFile` stream after the request completes.

## Risks / Trade-offs

- [Redis unavailable] → Catch Redis exceptions in the cache support layer, log warnings, and execute the original MySQL-backed path.
- [Stale user recommendations] → Increment the user recommendation version on every relevant behavior mutation and use short TTLs.
- [Stale public or configuration data] → Add targeted administrative invalidation and use TTLs as a bounded fallback.
- [Cache stampede] → Accept short-term duplicate computation in this local deployment; log cache misses. Add distributed locking later only if measured necessary.
- [Too many parallel database reads] → Keep the recommendation pool smaller than or comparable to the database connection limit and use an aggregate timeout.
- [Executor rejection] → Use bounded queues and explicit policies; return a meaningful upload failure or fall back for recommendation work.
- [Multipart stream lifetime] → Copy upload input before dispatching to `uploadExecutor`.
- [Async transaction loss] → Keep database mutations in existing transactional service methods and pass immutable IDs/values into worker tasks.
- [Redis memory growth] → Set a finite TTL on every cache entry and do not cache chapter text.

## Migration Plan

1. Correct Redis configuration and add the cache support component with caching disabled or enabled through `app.cache.enabled`.
2. Add the three executor beans and remove the unusable thread-pool class.
3. Add recommendation, effective-weight, public-content, configuration, and tag-heat cache paths.
4. Add user and administrative invalidation hooks.
5. Connect recommendation reads, EPUB processing, and tag-heat recomputation to their executors.
6. Validate startup, cache behavior, degradation, and executor behavior.

Rollback is available by setting `app.cache.enabled=false` and temporarily restoring synchronous execution paths. Redis remains non-authoritative, so no data migration or schema change is required.
