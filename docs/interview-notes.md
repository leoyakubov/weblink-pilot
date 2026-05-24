# Interview Notes

## Why not start with microservices

The project intentionally starts as a modular monolith because it keeps delivery velocity high while preserving service boundaries. The analytics path already uses events, so it can later be extracted into a standalone service with Kafka.

## Key trade-offs

- Random short codes give better unpredictability for default links, while custom aliases remain optional for branded campaigns.
- Custom aliases are supported through a unique constraint and validation.
- Cache-aside lookup keeps redirects fast.
- Asynchronous analytics protects redirect latency.

## Questions to expect

- Why random codes instead of database-derived codes?
- How would you scale redirects to millions of requests?
- How would you prevent hot-key overload in Redis?
- What changes when analytics becomes a separate service?