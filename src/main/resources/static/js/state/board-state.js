/* Client-side source of truth. It deliberately has no HTTP or DOM dependency. */
export const Mode = Object.freeze({ SELECT: 'select', CONNECT: 'connect' });
export const RemoteStatus = Object.freeze({ IDLE: 'idle', LOADING: 'loading', SUCCESS: 'success', ERROR: 'error' });
const CONNECTOR = 'CONNECTOR';

function newId(prefix) {
  const random = globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
  return `${prefix}-${random}`;
}

function initialState() {
  return { board: { id: null, name: '', elements: [] }, selectedId: null, mode: Mode.SELECT, connectSourceId: null,
    dirty: false, message: null, remote: { status: RemoteStatus.IDLE, operation: null, error: null } };
}

export function createBoardState() {
  let state = initialState();
  const listeners = new Set();
  const commit = changes => { state = { ...state, ...changes }; const snapshot = structuredClone(state); listeners.forEach(listener => listener(snapshot)); };
  const find = id => state.board.elements.find(element => element.id === id);
  const isShape = element => element !== undefined && element.type !== CONNECTOR;
  const changeElements = (elements, selectedId, extra = {}) => commit({ board: { ...state.board, elements }, selectedId, dirty: true, message: null, ...extra });
  function addShape(shape) {
    const count = state.board.elements.filter(isShape).length;
    const element = { ...shape, x: 40 + (count % 5) * 190, y: 40 + (Math.floor(count / 5) % 5) * 110, sourceId: null, targetId: null };
    changeElements([...state.board.elements, element], element.id);
  }
  const updateShape = (id, changes) => changeElements(state.board.elements.map(e => (e.id === id ? { ...e, ...changes } : e)), id);

  return {
    subscribe(listener) { listeners.add(listener); listener(structuredClone(state)); },
    isBusy: () => state.remote.status === RemoteStatus.LOADING,
    isConnecting: () => state.mode === Mode.CONNECT,
    lastOperation: () => state.remote.operation,
    toPersistedBoard: () => structuredClone(state.board),
    rename(name) { if (state.board.id !== null && name !== state.board.name) commit({ board: { ...state.board, name }, dirty: true }); },
    select(id) { if (id !== state.selectedId && (id === null || find(id))) commit({ selectedId: id }); },
    addRectangle() { addShape({ id: newId('rect'), type: 'RECTANGLE', width: 170, height: 70, text: 'Componente' }); },
    addText() { addShape({ id: newId('text'), type: 'TEXT', width: 160, height: 32, text: 'Texto' }); },
    moveElement(id, x, y) { const element = find(id); if (isShape(element) && (element.x !== x || element.y !== y)) updateShape(id, { x, y }); },
    editText(id, text) { const element = find(id); if (isShape(element) && element.text !== text) updateShape(id, { text }); },
    removeSelected() {
      const removed = state.selectedId;
      if (removed === null) return;
      changeElements(state.board.elements.filter(e => e.id !== removed && e.sourceId !== removed && e.targetId !== removed), null, { mode: Mode.SELECT, connectSourceId: null });
    },
    startConnect() { if (!find(state.selectedId)) return false; commit({ mode: Mode.CONNECT, connectSourceId: state.selectedId, message: null }); return true; },
    cancelConnect() { if (state.mode === Mode.CONNECT) commit({ mode: Mode.SELECT, connectSourceId: null }); },
    completeConnect(targetId) {
      const sourceId = state.connectSourceId;
      if (state.mode !== Mode.CONNECT || targetId === sourceId || !find(targetId)) return 'invalid';
      const done = { mode: Mode.SELECT, connectSourceId: null };
      const duplicate = state.board.elements.some(e => e.type === CONNECTOR && [e.sourceId, e.targetId].includes(sourceId) && [e.sourceId, e.targetId].includes(targetId));
      if (duplicate) { commit({ ...done, selectedId: targetId }); return 'duplicate'; }
      const connector = { id: newId('conn'), type: CONNECTOR, x: 0, y: 0, width: 0, height: 0, text: '', sourceId, targetId };
      changeElements([...state.board.elements, connector], connector.id, done);
      return 'created';
    },
    remoteStarted(operation, text) { commit({ remote: { status: RemoteStatus.LOADING, operation, error: null }, message: { text, kind: 'info' } }); },
    remoteSucceeded(board, text) {
      const sameBoard = board.id === state.board.id;
      const keepSelection = sameBoard && board.elements.some(e => e.id === state.selectedId);
      commit({ board: { id: board.id, name: board.name, elements: board.elements }, selectedId: keepSelection ? state.selectedId : null, mode: Mode.SELECT, connectSourceId: null, dirty: false, remote: { ...state.remote, status: RemoteStatus.SUCCESS, error: null }, message: { text, kind: 'success' } });
    },
    remoteFailed(error, text) { commit({ remote: { ...state.remote, status: RemoteStatus.ERROR, error }, message: { text, kind: 'error' } }); },
    notify(text, kind = 'info') { commit({ message: { text, kind } }); },
  };
}
