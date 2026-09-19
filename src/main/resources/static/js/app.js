import { BoardApiClient } from './api/board-api-client.js';
import { createBoardState } from './state/board-state.js';
import { createBoardView } from './ui/board-view.js';

/*
 * BoardApp: orchestration only. It turns view intents into state changes and remote
 * operations. It builds no HTTP request and touches no DOM node itself.
 */
const state = createBoardState();
const view = createBoardView(document);

/** Ignores an intent while a remote operation is running (incompatible actions). */
const whenIdle = action => (...args) => {
  if (!state.isBusy()) action(...args);
};

// ---- Part 1: local interaction. The state changes first; the view re-renders from it. ----

view.on({
  addRectangle: whenIdle(() => state.addRectangle()),
  addText: whenIdle(() => state.addText()),
  editText: whenIdle((id, text) => state.editText(id, text)),
  move: whenIdle((id, x, y) => state.moveElement(id, x, y)),
  removeSelected: whenIdle(() => state.removeSelected()),
  rename: whenIdle(name => {
    if (name) state.rename(name);
    else state.notify('El nombre del board no puede quedar vacío.', 'error');
  }),
  pressBackground: whenIdle(() => {
    state.cancelConnect();
    state.select(null);
  }),
  pressElement: whenIdle(id => {
    if (!state.isConnecting()) {
      state.select(id);
      return;
    }
    const result = state.completeConnect(id);
    if (result === 'created') state.notify('Conector creado. Use Guardar para persistirlo.', 'success');
    if (result === 'duplicate') state.notify('Esos elementos ya están conectados.');
    if (result === 'invalid') state.notify('Elija un elemento distinto al de origen.', 'error');
  }),
  toggleConnect: whenIdle(() => {
    if (state.isConnecting()) state.cancelConnect();
    else if (!state.startConnect()) state.notify('Seleccione primero el elemento de origen.', 'error');
  }),
  cancel: () => state.cancelConnect(),
});

state.subscribe(view.render);

// ---- Part 2: remote operations with loading / success / error / retry. ----

/*
 * Each remote operation is plain data ({ type, ... }), so the state can keep the last
 * one and Retry runs exactly the same operation again.
 */
const remoteOperations = {
  create: {
    call: ({ name }) => BoardApiClient.create(name),
    running: 'Creando board…',
    done: board => `Board creado. boardId: ${board.id}`,
    failed: 'No se pudo crear el board',
  },
  load: {
    call: ({ boardId }) => BoardApiClient.load(boardId),
    running: 'Cargando board…',
    done: board => `Board "${board.name}" cargado con ${board.elements.length} elementos.`,
    failed: 'No se pudo cargar el board',
  },
  save: {
    // Save sends the current local board, so a retry persists what the user sees now.
    call: () => BoardApiClient.save(state.toPersistedBoard()),
    running: 'Guardando board…',
    done: board => `Board guardado con ${board.elements.length} elementos.`,
    failed: 'No se pudo guardar el board',
  },
};

async function run(operation) {
  if (state.isBusy()) return;
  const spec = remoteOperations[operation.type];
  state.remoteStarted(operation, spec.running);
  try {
    const board = await spec.call(operation);
    state.remoteSucceeded(board, spec.done(board));
  } catch (error) {
    // Plain data in the state (not the Error object), so snapshots stay cloneable.
    const failure = {
      status: error.status ?? 0,
      code: error.code ?? 'UNEXPECTED_ERROR',
      message: error.message,
    };
    const detail = failure.status ? `${failure.code}, HTTP ${failure.status}` : failure.code;
    state.remoteFailed(failure, `${spec.failed}: ${failure.message} (${detail})`);
  }
}

view.on({
  createBoard: whenIdle(name => {
    if (name) run({ type: 'create', name });
    else state.notify('Escriba un nombre para el nuevo board.', 'error');
  }),
  loadBoard: whenIdle(boardId => {
    if (boardId) run({ type: 'load', boardId });
    else state.notify('Escriba el boardId que desea cargar.', 'error');
  }),
  save: whenIdle(() => run({ type: 'save' })),
  retry: whenIdle(() => {
    const operation = state.lastOperation();
    if (operation) run(operation);
  }),
});

// Reloading the page (F5) brings back the board referenced in the URL (#boardId).
const initialBoardId = view.initialBoardId();
if (initialBoardId) run({ type: 'load', boardId: initialBoardId });
