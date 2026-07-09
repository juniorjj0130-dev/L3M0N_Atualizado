---
name: l3mon-maintainer
description: 'Support repeatable maintenance workflows for the L3MON repository, including server-side Node.js fixes, Express route updates, Socket.IO behavior, EJS view changes, and Android client integration.'
argument-hint: 'Describe the L3MON task, bug fix, or refactor you want to implement.'
---

# L3MON Maintainer

## When to Use
- Add or refactor server-side behavior in `server/index.js` or `server/includes/`
- Debug route handling, socket events, and database access in the Express app
- Update rendered views or UI flow under `server/assets/views/`
- Track Android client integration and build logic in `client/`

## Procedure
1. Identify the target behavior from the request: server route, socket message, view change, or Android client flow.
2. Locate the entry point:
   - `server/index.js` for app startup and middleware wiring
   - `server/includes/expressRoutes.js`, `databaseGateway.js`, `clientManager.js`, or `logManager.js` for business logic
   - `server/assets/views/` for EJS template updates
   - `client/app/src/main/java/` for Android client implementation
3. Trace the flow from entry point to implementation, preserving existing conventions and limiting changes to the smallest effective scope.
4. Make edits in the relevant module and update associated views, routes, or handlers as needed.
5. Validate the change with lightweight checks such as syntax validation, `npm test`, or running the server locally.

## Quality Criteria
- Preserve existing route and event names
- Avoid introducing new dependencies unless needed
- Keep changes small and explicit
- Document any non-obvious behavior in comments or in the skill notes

## Notes
- Prefer targeted fixes over broad rewrites
- Use the repository structure as a guide: `server/` for backend logic, `client/` for Android integration, `assets/views/` for UI templates
