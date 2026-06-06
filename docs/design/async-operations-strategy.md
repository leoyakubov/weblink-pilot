# Async And Non-Blocking Operations Strategy

## Purpose

This document is the working guide for deciding which flows should stay synchronous and which ones should become asynchronous or non-blocking later.

The goal is to keep the product fast and simple where it matters, while moving only the right side effects off the request path.

## Core Principle

- Keep user-facing actions synchronous when the user is waiting for the response.
- Move side effects, fan-out, notifications, and batch work to async processing when it improves latency or resilience.
- Prefer explicit events and background jobs over premature reactive complexity.
- Keep PostgreSQL as the durable source of truth.
- Treat Redis as a performance layer, not the system of record.

## Current Default

Our current default is:

- synchronous HTTP request handling for the main product flows
- synchronous repository access for durable writes and reads
- asynchronous domain events for click analytics, auth email notifications, and future fan-out
- scheduled jobs for maintenance tasks

## Implemented Async Paths

- auth email verification and password reset notifications are published as domain events after the surrounding transaction commits
- the actual SMTP send is handled by an async listener, not on the request thread

That default keeps the app easy to reason about and fits the current stack.

## Good Async Candidates

| Flow | Why async helps | Suggested mechanism |
|---|---|---|
| Email delivery for auth workflows | login/reset/verification should return quickly even if SMTP is slow | event handler or background job, now implemented for reset/verification mail |
| Expiry reminder emails | batch work does not belong on the request path | scheduled job + async mail dispatch |
| Click analytics fan-out | redirect latency should stay low | application event now, broker later if needed |
| Cache invalidation / warming | side effects should not delay user-visible work | event listener or deferred job |
| Future notifications | multiple consumers may need the same event | event publishing, then queue later if needed |
| Heavy read-model rebuilds | aggregate updates can be rebuilt off the hot path | scheduled job or worker |

## Good Sync Candidates

| Flow | Why stay sync | Notes |
|---|---|---|
| Login and refresh | user waits for the result | keep token rotation deterministic |
| Logout | must revoke immediately | user expects immediate effect |
| Create link | user needs the short URL right away | async should only cover side effects |
| Redirect | latency is the product | keep the decision path short |
| Profile reads | simple request-response fit | optimize with cache if needed |

## Decision Matrix

Ask these questions before moving a flow async:

1. Does the user need the result immediately?
2. Is the work a side effect rather than the core action?
3. Can the work be retried safely?
4. Is the work independent of the request transaction?
5. Would async reduce latency or improve resilience?

If the answer is mostly yes, the flow is a candidate.

## Communication Patterns

### Synchronous

- frontend HTTP calls to backend APIs
- service calls inside the same module
- repository access to PostgreSQL
- hot cache reads/writes to Redis

### Asynchronous

- module events for click tracking and fan-out
- scheduled jobs for reminder scans and cleanup
- later broker-backed queues if internal events stop being enough

## Candidate Areas In This Project

### Auth

Implemented:

- send email verification mail asynchronously
- send password reset mail asynchronously

Potential async follow-ups:

- eventually queue login side effects that are not required for immediate response

### Links

Potential async follow-ups:

- publish link created / clicked events
- defer cache warming or invalidation where safe
- offload cleanup or archive work if the table grows

### Analytics

Potential async follow-ups:

- consume link click events asynchronously
- batch or queue expensive enrichments
- move summary rebuilds off the request path

### Operations

Potential async follow-ups:

- expiry reminder emails
- future maintenance jobs
- periodic health or audit reports

## Non-Blocking Notes

We do not currently need a full reactive stack.

Non-blocking is only worth it if:

- the request volume is high enough to justify the complexity
- the current blocking I/O becomes a measurable bottleneck
- the team wants to adopt a reactive programming model consistently

Until then, async events and background jobs are the right fit.

## Practical Rule

If a flow changes what the user sees immediately, keep the core of that flow synchronous.
If a flow mainly performs side effects, move that part to async processing.
