# API Deprecation Checklist

Step-by-step checklist for safely deprecating an API endpoint.

## Pre-Deprecation

- [ ] Identify all consumers of the endpoint (API keys, access logs, contracts)
- [ ] Design the replacement endpoint or alternative approach
- [ ] Write migration guide with code examples
- [ ] Set deprecation timeline (minimum 6 months for public APIs)
- [ ] Get stakeholder approval for deprecation plan

## Announce Phase (T-6 months)

- [ ] Publish API changelog entry with deprecation notice
- [ ] Send email notification to registered API consumers
- [ ] Add deprecation banner to developer portal / dashboard
- [ ] Update API documentation to mark endpoint as deprecated
- [ ] Create migration guide with before/after examples

## Warn Phase (T-3 months)

- [ ] Add `Deprecation: true` response header to endpoint
- [ ] Enable usage logging to track remaining consumers
- [ ] Send reminder notifications to active consumers
- [ ] Offer migration support (office hours, dedicated channel)
- [ ] Review consumer migration progress

## Sunset Phase (T-0)

- [ ] Add `Sunset: <date>` header (RFC 8594) with removal date
- [ ] Send final migration deadline notification
- [ ] Contact remaining high-traffic consumers directly
- [ ] Prepare 410 Gone response for post-removal
- [ ] Verify replacement endpoint handles expected load

## Remove Phase (T + grace period)

- [ ] Switch endpoint to return 410 Gone
- [ ] Include migration link in 410 response body
- [ ] Monitor error rates for consumers still hitting old endpoint
- [ ] Remove endpoint code after grace period
- [ ] Archive API documentation (do not delete)

## Post-Removal

- [ ] Verify no internal services depend on removed endpoint
- [ ] Update API version matrix / compatibility table
- [ ] Document lessons learned for future deprecations
- [ ] Close deprecation tracking ticket
