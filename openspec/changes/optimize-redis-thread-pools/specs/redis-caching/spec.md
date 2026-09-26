## Purpose

Provides Redis-backed caching for expensive backend reads while keeping MySQL as the authoritative data source and preserving behavior when Redis is unavailable.

## ADDED Requirements

### Requirement: Cache can be disabled
The system SHALL allow caching to be disabled by configuration. When caching is disabled, supported reads MUST bypass Redis and retrieve data from the existing MySQL-backed behavior.

#### Scenario: Cache is disabled
- **WHEN** caching is disabled with `app.cache.enabled=false`
- **THEN** recommendation, public content, configuration, and tag-heat reads bypass Redis and return data from the existing MySQL-backed behavior

### Requirement: Redis failures degrade to MySQL
The system SHALL keep supported core reads available when Redis is unavailable. A Redis connection or command failure MUST NOT cause a supported read request to fail when the same request can be served by the existing MySQL-backed behavior.

#### Scenario: Redis is unavailable during a read
- **WHEN** caching is enabled but Redis is unavailable
- **THEN** the system serves recommendation, public book, recommendation-configuration, and tag-heat reads from the existing MySQL-backed behavior
- **AND** the backend records a warning without returning an unhandled Redis error to the client

### Requirement: Recommendation results are cached per user and sort
The system SHALL cache recommendation results independently by user, sort type, and user recommendation version. A cache entry MUST expire after its configured TTL.

#### Scenario: A recommendation result is requested twice
- **WHEN** the same user requests the same sort type before the cache entry expires and no relevant user behavior has changed
- **THEN** the system returns the cached recommendation result without recomputing it from MySQL

#### Scenario: A recommendation cache entry expires
- **WHEN** the recommendation cache TTL has elapsed
- **THEN** the next request recomputes the recommendation and stores the new result with a fresh TTL

### Requirement: User behavior invalidates recommendation caches
The system SHALL use a user recommendation version that changes when behavior affecting recommendations changes. Changed behavior MUST prevent older user-scoped recommendation results and effective tag weights from being served.

#### Scenario: A user updates recommendation-relevant behavior
- **WHEN** a collection, reading history, finished novel, comment, dislike, preference-tag, or recommendation-profile change is saved
- **THEN** the user's recommendation version changes
- **AND** subsequent recommendation and effective-tag-weight reads use the new version rather than the previous version's cached values

### Requirement: Effective tag weights are cached per user version
The system SHALL cache effective tag weights by user and recommendation version and MUST fall back to the existing calculation when the cache is unavailable.

#### Scenario: Effective tag weights are requested twice
- **WHEN** the same user's effective tag weights are requested before the cache expires and the user's recommendation version has not changed
- **THEN** the system returns the cached weights without rereading all underlying behavior and novel data

### Requirement: Public content and recommendation configuration are cached
The system SHALL cache carousel data, category book lists, book details, and recommendation configuration entries with configured TTLs. Administrative mutations that change the underlying data MUST prevent stale cached values from being returned.

#### Scenario: Public content is requested twice
- **WHEN** the same carousel, category list, or book detail is requested before its cache entry expires
- **THEN** the system returns the cached value without querying MySQL for that value

#### Scenario: An administrator updates cached public data
- **WHEN** an administrator creates, updates, deletes, or changes the status of data backing a public cache entry
- **THEN** the affected cache entry is no longer served
- **AND** the next request retrieves the updated data from MySQL

### Requirement: Tag heat results are cached
The system SHALL cache site-wide tag-heat results and refresh them after the configured TTL. A cache miss MUST NOT permanently prevent tag-heat data from being returned.

#### Scenario: Tag heat is requested twice
- **WHEN** tag heat is requested before the cache expires
- **THEN** the system returns the cached heat result without rescanning all user behavior data

#### Scenario: Tag-heat cache expires
- **WHEN** the tag-heat cache TTL has elapsed
- **THEN** the system recomputes tag heat and makes the refreshed result available for later requests

### Requirement: Cached data remains bounded by TTL
The system SHALL assign a TTL to every Redis cache entry used by this capability. The system MUST NOT create a cache entry that never expires.

#### Scenario: Cache entries are written
- **WHEN** any Redis cache entry for this capability is created
- **THEN** the entry has a configured non-infinite expiration time
