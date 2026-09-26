## 1. Configuration Foundation

- [x] 1.1 Move Redis connection settings to `spring.data.redis`, configure host, port, password, command timeout, and connect timeout through environment-variable defaults.
- [x] 1.2 Add the application cache switch and cache TTL configuration while keeping MySQL behavior available when caching is disabled.
- [x] 1.3 Add a centralized Redis cache component using `StringRedisTemplate`, JSON serialization, the `novel:` key namespace, finite TTLs, and warning-level failure logging.
- [x] 1.4 Replace the existing `ThreadPool` class with a Spring `@Configuration` class defining `recommendExecutor`, `uploadExecutor`, and `statsExecutor`.
- [x] 1.5 Configure bounded queues, keep-alive times, thread-name prefixes, explicit rejection policies, and graceful shutdown for all three executors.

## 2. Redis Cache Integration

- [x] 2.1 Add a per-user recommendation version key and helper operations for reading and incrementing the version.
- [x] 2.2 Add recommendation-result caching by user, sort type, and recommendation version, including `cachedAt` metadata and a finite TTL.
- [x] 2.3 Add effective-tag-weight caching by user and recommendation version, reusing the same user version boundary.
- [x] 2.4 Increment the user recommendation version after collection, reading-history, finished-novel, comment, dislike, preference-tag, and recommendation-profile mutations.
- [x] 2.5 Add Redis caching for carousel data, category book lists, and book details.
- [x] 2.6 Add Redis caching for main and comment recommendation configuration.
- [x] 2.7 Add targeted cache invalidation to administrative novel, banner, and recommendation-configuration mutation paths.
- [x] 2.8 Move site tag-heat results from local in-memory state to Redis with a finite TTL.
- [x] 2.9 Ensure every Redis-enabled read falls back to the existing MySQL-backed behavior when Redis is disabled or unavailable.

## 3. Thread Pool Integration

- [x] 3.1 Identify and group independent recommendation data reads into parallel-safe loading tasks.
- [x] 3.2 Execute independent recommendation reads on `recommendExecutor`, aggregate them with a bounded timeout, and use the existing sequential path as a fallback when parallel loading fails.
- [x] 3.3 Copy uploaded EPUB input before dispatching background-friendly work and process accepted files concurrently on `uploadExecutor`.
- [x] 3.4 Preserve the existing synchronous upload response while reporting successful imports and isolating individual file failures.
- [x] 3.5 Move tag-heat recomputation to `statsExecutor`, serve stale Redis values during refresh when available, and use synchronous fallback computation only when no cached value exists.
- [x] 3.6 Ensure task submissions pass immutable values or IDs rather than relying on request-thread context.

## 4. Observability

- [x] 4.1 Log Redis cache hits, misses, write failures, read failures, and invalidation operations at appropriate levels.
- [x] 4.2 Log executor task failures with workload-specific thread-name context.
- [x] 4.3 Log or otherwise record executor rejection events so saturated pools are visible during testing.
- [x] 4.4 Record recommendation request cache status and execution time for manual performance comparison.

## 5. Validation

- [x] 5.1 Run the backend test suite or, if no tests exist, run a backend compile/package check.
- [x] 5.2 Verify application startup with the corrected Redis configuration and all three executor beans registered.
- [x] 5.3 Verify recommendation and effective-weight cache hits, TTL expiration, and user-version invalidation. <!-- Verified with a 10s recommend-TTL runtime test: MISS -> keys written (novel:rec:result / novel:rec:weights, TTL 9s) -> HIT with unchanged cachedAt -> keys expire after TTL -> next request recomputes MISS and writes fresh TTL. See docs/redis-thread-pool-verification.md. -->
- [x] 5.4 Verify carousel, category, detail, configuration, and tag-heat cache hits plus administrative invalidation.
- [x] 5.5 Verify supported core reads still return MySQL-backed data when Redis is disabled and when Redis is unavailable.
- [x] 5.6 Verify concurrent recommendation reads, concurrent EPUB processing, and off-request tag-heat recomputation use their designated executors.
- [x] 5.7 Verify bounded-queue rejection behavior and graceful executor shutdown. <!-- Runtime smoke test over the real ExecutorConfig beans: uploadExecutor accepted=24 rejected=5 at capacity (max 4 + queue 20) with workload-specific rejection WARN log; statsExecutor shutdown() waited ~2.5s for an in-progress task. See docs/redis-thread-pool-verification.md. -->
- [x] 5.8 Run OpenSpec validation for the change and resolve any reported artifact issues.
