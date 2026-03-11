---
name: api-design
description: >
  Knowledge Pack: API Design -- REST patterns, HTTP status codes, RFC 7807
  error responses, pagination, content negotiation, validation, versioning
  strategies, and request/response shaping for my-cli-tool.
---

# Knowledge Pack: API Design

## Summary

API design conventions for my-cli-tool using python 3.9 with click.

### URL Structure

- Base path: `/api/v{version}/{resource}`
- Resources: plural nouns, lowercase with hyphens (`/api/v1/bank-accounts`)
- Sub-resources: `/api/v1/customers/{id}/orders`
- Actions (non-CRUD): POST to `/api/v1/orders/{id}/cancel`

### HTTP Status Codes

- **Success**: 200 (GET/PUT/PATCH), 201 (POST created), 204 (DELETE)
- **Client Error**: 400 (validation), 404 (not found), 409 (conflict), 422 (business rule), 429 (rate limit)
- **Server Error**: 500 (unexpected)

### Error Responses (RFC 7807)

All errors return `application/problem+json` with: `type`, `title`, `status`, `detail`, `instance`.

### Pagination and Validation

- Offset-based: `?page=1&limit=20` (default 20, max 100) or cursor-based for large datasets
- Validate at API boundary, return all errors in a single RFC 7807 response
- Versioning via URL path (`/v1/`), content type `application/json`

## References

- `.github/skills/api-design/SKILL.md` -- Full API design reference
