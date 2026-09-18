/* BoardApiClient is the only module that knows the HTTP contract. */
const BASE_PATH = '/api/boards';
const TIMEOUT_MS = 10000;

export class BoardApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = 'BoardApiError';
    this.status = status;
    this.code = code;
  }
}

async function request(method, path, body) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(TIMEOUT_MS) });
  } catch (error) {
    if (error.name === 'TimeoutError') throw new BoardApiError(0, 'TIMEOUT', 'El servidor no respondió a tiempo');
    throw new BoardApiError(0, 'NETWORK_ERROR', 'No fue posible conectar con el servidor');
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new BoardApiError(response.status, payload?.code ?? 'HTTP_ERROR', payload?.message ?? `Respuesta HTTP ${response.status}`);
  return payload;
}

function boardPath(boardId) {
  const id = String(boardId ?? '').trim();
  if (!id) throw new BoardApiError(0, 'MISSING_BOARD_ID', 'Falta el boardId');
  return `${BASE_PATH}/${encodeURIComponent(id)}`;
}

export const BoardApiClient = {
  create: name => request('POST', BASE_PATH, { name }),
  load: boardId => request('GET', boardPath(boardId)),
  save: board => request('PUT', boardPath(board.id), { name: board.name, elements: board.elements }),
};
