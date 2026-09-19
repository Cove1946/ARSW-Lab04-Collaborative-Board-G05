# ADR-002 — Client boundaries for the interactive Board

- Status: Accepted
- Date: 2026-09-18

## Context

Lab 05 adds an interactive SVG client without changing the three REST operations established in Lab 04. The client must create, load, edit and persist a complete Board; it also needs visible loading, error and retry feedback. Mixing HTTP, DOM manipulation and Board rules in one script would make those responsibilities hard to test and evolve.

## Decision

The browser client is separated into four ES modules:

- `BoardApiClient` is the only module that uses `fetch`. It owns `/api/boards`, JSON conversion, timeout and the `BoardApiError` translation.
- `BoardState` is the local source of truth. It owns immutable Board updates, selection, connection mode, local connector rules and remote-operation data.
- `BoardView` renders state into SVG/DOM and turns browser gestures into intents. It does not call HTTP or store business state.
- `BoardApp` is the composition root. It connects view intents with state actions and API operations, including Retry.

The server remains the authority for domain invariants. The browser repeats simple interaction rules (for example, no self-connection) only to give immediate feedback.

## Consequences

- The only permitted `fetch` occurrence is in `js/api/board-api-client.js`.
- State snapshots consist only of plain data, so they can be copied with `structuredClone`; callbacks and Error instances are not stored in state.
- The view can be changed from SVG to another presentation without changing HTTP code or Board rules.
- Full Board persistence remains one `PUT`; moves, connections and deletion do not create new endpoints.

## Trade-offs

The separation introduces small modules and explicit intent wiring that can feel verbose for a small application. Rendering the SVG from complete state snapshots is simple and reliable, but a large Board could later need incremental rendering. Native `prompt` and `confirm` simplify the Lab 05 interface but do not provide custom visual styling.

## Evidence

- `mvn test`: 40 tests passing — the 12 delivered in Lab 04 are all still present, plus 28 added for CONNECTOR invariants, the updated REST contract, the error-handling fixes and integration.
- Domain, REST and integration tests cover connector persistence and rejected invalid replacements.
- The boundary is checkable with a search: `fetch(` appears only in `static/js/api/board-api-client.js`, and `board-view.js` has no imports at all, so it cannot reach the API.
- The API client converts HTTP, network and timeout failures to one error shape.
- Manual verification covers creating, loading, moving, connecting, saving, reloading and retrying a failed operation.
