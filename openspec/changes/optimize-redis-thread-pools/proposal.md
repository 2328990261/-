## Why

The backend has a Redis dependency and a thread-pool class, but neither is currently usable in business flows: Redis connection properties are under the wrong Spring Boot namespace, and the thread pool is neither registered as a Spring configuration nor safely isolated by workload. Recommendation generation, public book queries, EPUB import, and tag-heat recomputation can therefore place avoidable load on MySQL and request threads.

## What Changes

- Correct the Spring Boot Redis connection configuration and add a reusable Redis cache helper with explicit keys, TTLs, JSON serialization, logging, and a cache-enabled switch.
- Add versioned Redis caching for personalized recommendation results, effective tag weights, public carousel/category/detail queries, recommendation configuration, and tag heat.
- Invalidate user-scoped recommendation caches when relevant user behavior changes, and public/config caches when administrators mutate the underlying data.
- Degrade to the existing MySQL-backed behavior when Redis is unavailable or caching is disabled.
- Replace the unusable thread-pool class with Spring-managed, workload-isolated executors for recommendation queries, EPUB import, and statistics recomputation.
- Use bounded queues, explicit rejection policies, thread-name prefixes, and graceful shutdown for all business executors.
- Keep EPUB upload synchronous in this change while parallelizing internal file processing; do not introduce a frontend task center.
- Exclude verification-code storage from this change.

## Capabilities

### New Capabilities

- `redis-caching`: Redis-backed caching and degradation behavior for recommendation, user preference, public content, configuration, and tag-heat reads.
- `business-thread-pools`: Spring-managed workload-isolated thread pools and their use by recommendation queries, EPUB import, and statistics recomputation.

### Modified Capabilities

None. The project currently has no established specs.

## Impact

- Backend configuration:
  - `springboot/src/main/resources/application.yml`
- Backend configuration and services:
  - Replace the existing `ThreadPool` class with executor configuration.
  - Add Redis cache support.
  - Modify recommendation, public book, tag-heat, and EPUB import flows.
  - Add cache invalidation hooks to relevant user-behavior and administrative write paths.
- Dependencies:
  - The existing `spring-boot-starter-data-redis` dependency is used; no new dependency is required for the base implementation.
- Operational behavior:
  - Redis becomes a cache and temporary-state layer only; MySQL remains the source of truth.
  - Core read paths must continue to work when Redis is unavailable.
