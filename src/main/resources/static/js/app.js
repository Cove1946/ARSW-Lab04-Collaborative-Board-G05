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
