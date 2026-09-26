## Purpose

Isolates recommendation queries, EPUB import processing, and statistics recomputation into independently managed bounded worker pools so one workload cannot consume another workload's execution capacity.

## ADDED Requirements

### Requirement: Business workloads use isolated executors
The system SHALL execute recommendation data loading, EPUB file processing, and tag-heat recomputation on separate workload-specific worker pools. A saturated or slow workload MUST NOT consume worker capacity reserved for another workload.

#### Scenario: EPUB processing is saturated
- **WHEN** all EPUB-processing workers are busy and the upload queue is full
- **THEN** the system does not consume workers reserved for recommendation queries or tag-heat recomputation

### Requirement: Worker pools have bounded capacity
Each business worker pool SHALL have a bounded queue and an explicit rejection policy. Rejected work MUST be reported or logged without leaving an unhandled background failure.

#### Scenario: A worker queue is full
- **WHEN** a task is submitted after a business worker pool has reached its configured worker and queue limits
- **THEN** the system applies the pool's explicit rejection policy
- **AND** the backend logs or returns a meaningful result rather than silently discarding the task

### Requirement: Worker pools support graceful shutdown
Each business worker pool SHALL allow in-progress tasks to finish during application shutdown, subject to a configured maximum wait time.

#### Scenario: The application shuts down while tasks are running
- **WHEN** the backend receives a shutdown request while business tasks are in progress
- **THEN** the worker pools wait for those tasks up to their configured shutdown timeout before terminating

### Requirement: Recommendation queries can run concurrently
The system SHALL run independent recommendation data reads concurrently and aggregate their results before producing a recommendation response.

#### Scenario: Independent recommendation reads are submitted
- **WHEN** a recommendation request needs multiple independent data reads
- **THEN** those reads execute concurrently on the recommendation worker pool
- **AND** the system waits for the required results before producing the final recommendation

#### Scenario: One concurrent recommendation read fails
- **WHEN** one independent recommendation read fails while other required reads complete
- **THEN** the system logs the failed read
- **AND** the recommendation request still completes by using a fallback execution path when possible

### Requirement: EPUB import remains synchronous while processing files concurrently
The system SHALL keep the existing synchronous upload response contract while processing accepted EPUB files concurrently. A failure in one file MUST NOT prevent other valid files from being processed and reported.

#### Scenario: Multiple EPUB files are uploaded
- **WHEN** an upload contains multiple valid EPUB files
- **THEN** the files are processed concurrently within the EPUB worker pool's configured limits
- **AND** the upload response reports the number of successfully imported volumes

#### Scenario: One EPUB file is invalid
- **WHEN** one file in a multi-file upload cannot be parsed
- **THEN** the system records that file's failure
- **AND** other valid files in the same upload are still proces

### Requirement: Tag heat recomputation runs off the request path
The system SHALL perform tag-heat recomputation on the statistics worker pool rather than on the web request worker when refreshed heat data is needed.

#### Scenario: Tag-heat cache expires
- **WHEN** the tag-heat cache expires and a subsequent request requires refreshed data
- **THEN** recomputation is performed by the statistics worker pool rather than a web request worker

### Requirement: Worker activity is diagnosable
The system SHALL name business worker threads by workload and record task failures and rejection events.

#### Scenario: A business task fails
- **WHEN** a task running on a business worker pool throws an exception
- **THEN** the backend logs the failure with the workload's thread-name prefix