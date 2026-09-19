# REST Contract — Collaborative Board (Lab 04 + Lab 05)

- Base path: `/api/boards`
- Format: JSON (`Content-Type: application/json`)
- The server generates Board ids; the client generates element ids.

## Board resource

```json
{
  "id": "7c243ead-ec19-4589-8f89-b52d9680fb3e",
  "name": "Architecture Session",
  "elements": [
    { "id": "api", "type": "RECTANGLE", "x": 60, "y": 60, "width": 170, "height": 70,
      "text": "API", "sourceId": null, "targetId": null },
    { "id": "note", "type": "TEXT", "x": 300, "y": 60, "width": 160, "height": 32,
      "text": "Notes", "sourceId": null, "targetId": null },
    { "id": "c1", "type": "CONNECTOR", "x": 0, "y": 0, "width": 0, "height": 0,
      "text": "", "sourceId": "api", "targetId": "note" }
  ]
}
```

| Field | Type | RECTANGLE | TEXT | CONNECTOR |
|---|---|---|---|---|
| `id` | string | required and unique within the board | same | same |
| `type` | enum | `RECTANGLE` | `TEXT` | `CONNECTOR` |
| `x`, `y` | number | upper-left position | position | unused (`0`) |
| `width`, `height` | number ≥ 0 | size | text area size | unused (`0`) |
| `text` | string | optional label | content | unused |
| `sourceId`, `targetId` | string or null | must be null | must be null | required and different |

Board rules checked on every PUT:

- Elements cannot be null and their ids are unique.
- A connector must point to two existing elements of the same Board. An endpoint may itself be another connector.
- The ordering of elements is irrelevant.
- If a rule fails, the response is `400` and the stored Board remains unchanged.

## Operations

### POST /api/boards

Creates a Board. Request: `{ "name": "Architecture Session" }`.

Returns `201 Created`, the new Board with an empty `elements` array, and `Location: /api/boards/{id}`.

### GET /api/boards/{boardId}

Returns `200 OK` with the Board, or `404 BOARD_NOT_FOUND`.

### PUT /api/boards/{boardId}

Replaces the complete state. Elements not included in the request are removed.

```json
{
  "name": "Architecture Session",
  "elements": [
    { "id": "api", "type": "RECTANGLE", "x": 60, "y": 60, "width": 170, "height": 70, "text": "API" },
    { "id": "note", "type": "TEXT", "x": 300, "y": 60, "width": 160, "height": 32, "text": "Notes" },
    { "id": "c1", "type": "CONNECTOR", "sourceId": "api", "targetId": "note" }
  ]
}
```

Returns `200 OK` with the saved Board and preserves its id. A PUT never creates a missing Board: it returns `404 BOARD_NOT_FOUND`.

## Error contract

Every error has the following shape:

```json
{
  "timestamp": "2026-09-18T00:00:00Z",
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "Connector c1 must join two different elements",
  "path": "/api/boards/board-id"
}
```

| Status | Code | Meaning |
|---|---|---|
| 400 | `INVALID_REQUEST` | Missing or invalid request fields, including a null element |
| 400 | `INVALID_INPUT` | A domain invariant failed |
| 400 | `MALFORMED_REQUEST` | Missing or malformed JSON body |
| 404 | `BOARD_NOT_FOUND` | The requested Board does not exist |
| 404 | `NOT_FOUND` | Unknown route |
| 405 | `METHOD_NOT_ALLOWED` | Unsupported HTTP method |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Request body is not JSON |
| 500 | `INTERNAL_ERROR` | Unexpected server failure; implementation details are omitted |

### Connector rule messages

A broken connector rule is reported as `400 INVALID_INPUT` with the exact message produced by the domain:

| Rule | `message` |
|---|---|
| `sourceId` or `targetId` missing | `Connector {id} requires sourceId and targetId` |
| `sourceId` equal to `targetId` | `Connector {id} must join two different elements` |
| Endpoint does not exist in the Board | `Connector {id} references a missing element: {elementId}` |
| RECTANGLE or TEXT carrying references | `Only CONNECTOR elements can have sourceId/targetId: {id}` |

## Decisions and variations from the minimum contract

1. **`Location` header on `201`** (Lab 04): a compatible addition that states the URL of the created resource.
2. **`MALFORMED_REQUEST` and `INTERNAL_ERROR`** (Lab 04): cover invalid JSON and unexpected failures without exposing internal messages.
3. **Spring's own errors keep their real status** (fixed in Lab 05): 404, 405 and 415 are no longer turned into 500.
4. **Element rules reported as `INVALID_INPUT`** (fixed in Lab 05): even though Jackson detects the failure while constructing `BoardElement`, the real rule message is returned.
5. **`INVALID_REQUEST` lists every error, sorted** (fixed in Lab 05): the same request always produces the same message.
6. **`sourceId` / `targetId`** (Lab 05): new fields, optional for RECTANGLE and TEXT. A Lab 04 client that omits them keeps working; responses always include them (`null` when they do not apply).
7. **Unknown fields are ignored** (Spring Boot default): a tolerant reader that lets client and server evolve separately.
8. **No new endpoints**: the full-board PUT already persists moves and connectors.
9. **No version control**: the last PUT replaces the state; concurrency is addressed in Lab 07.

## Client-side errors (BoardApiClient)

The web client converts every failure into `BoardApiError { status, code, message }`. When an HTTP response
exists it uses the values from `ApiError`; otherwise it uses `status = 0` and one of these codes:

| `code` | When |
|---|---|
| `NETWORK_ERROR` | The server is unreachable |
| `TIMEOUT` | No response within 10 seconds |
| `MISSING_BOARD_ID` | A load or save was attempted without a `boardId` |
| `HTTP_ERROR` | Error response without an `ApiError` body |
