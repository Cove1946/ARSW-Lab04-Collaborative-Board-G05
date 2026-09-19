/*
 * BoardView: projects a state snapshot onto the page (controls + SVG) and turns user
 * gestures into intents for the app. It keeps no business data of its own: every render
 * starts from the snapshot it receives, and it never calls the API.
 */
const SVG_NS = 'http://www.w3.org/2000/svg';
const CANVAS = { width: 1000, height: 600 }; // same numbers as the viewBox in index.html
const MIN_SIZE = { width: 60, height: 24 };  // clickable area for elements saved without size

function svg(name, attributes) {
  const node = document.createElementNS(SVG_NS, name);
  for (const [key, value] of Object.entries(attributes)) {
    node.setAttribute(key, value);
  }
  return node;
}

function bounds(element) {
  return {
    x: element.x,
    y: element.y,
    width: Math.max(element.width, MIN_SIZE.width),
    height: Math.max(element.height, MIN_SIZE.height),
  };
}

function center(element) {
  const box = bounds(element);
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 };
}

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

function boardIdFromUrl() {
  try {
    return decodeURIComponent(location.hash.slice(1)).trim();
  } catch {
    return ''; // malformed #fragment typed by hand
  }
}

export function createBoardView(doc) {
  const byId = id => doc.getElementById(id);
  const canvas = byId('boardCanvas');
  const ui = {
    status: byId('remoteStatus'),
    message: byId('message'),
    info: byId('boardInfo'),
    boardId: byId('boardId'),
    name: byId('boardName'),
    create: byId('newBoardBtn'),
    load: byId('loadBtn'),
    save: byId('saveBtn'),
    retry: byId('retryBtn'),
    addRect: byId('addRectBtn'),
    addText: byId('addTextBtn'),
    editText: byId('editTextBtn'),
    connect: byId('connectBtn'),
    remove: byId('deleteBtn'),
  };

  let handlers = {};
  let current = null; // last snapshot received; only used to interpret gestures
  let shownBoardId = null;
  let drag = null;
  ui.boardId.value = boardIdFromUrl();

  const emit = (intent, ...args) => handlers[intent]?.(...args);
  const findElement = id => current?.board.elements.find(e => e.id === id);

  // ---------------- rendering: the DOM is a projection of the snapshot ----------------

  function render(snapshot) {
    current = snapshot;
    renderControls(snapshot);
    renderCanvas(snapshot);
  }

  function renderControls({ board, selectedId, mode, dirty, message, remote }) {
    const busy = remote.status === 'loading';
    const hasBoard = board.id !== null;
    const connecting = mode === 'connect';
    const selected = board.elements.find(e => e.id === selectedId);
    const shapeSelected = selected !== undefined && selected.type !== 'CONNECTOR';

    ui.status.textContent = remote.status;
    ui.status.className = `status status-${remote.status}`;

    const shown = connecting && !busy && message?.kind !== 'error'
      ? { text: 'Modo conexión: haga clic en el elemento destino (Esc para cancelar).', kind: 'info' }
      : message;
    ui.message.textContent = shown?.text ?? '';
    ui.message.className = `message ${shown?.kind ?? ''}`;

    ui.info.textContent = hasBoard
      ? `Board ${board.id} · ${board.elements.length} elementos${dirty ? ' · cambios sin guardar' : ''}`
      : 'Cree o cargue un board para empezar.';

    if (board.id !== shownBoardId) {
      // Only when another board arrives, so a half-typed id is never overwritten.
      shownBoardId = board.id;
      ui.boardId.value = board.id ?? '';
      history.replaceState(null, '', board.id ? `#${encodeURIComponent(board.id)}` : location.pathname);
    }
    if (doc.activeElement !== ui.name) {
      ui.name.value = board.name;
    }

    ui.create.disabled = busy;
    ui.load.disabled = busy;
    ui.boardId.disabled = busy;
    ui.name.disabled = busy || !hasBoard;
    ui.save.disabled = busy || !hasBoard;
    ui.retry.hidden = remote.status !== 'error' || remote.operation === null;
    ui.retry.disabled = busy;
    ui.addRect.disabled = busy || !hasBoard;
    ui.addText.disabled = busy || !hasBoard;
    ui.editText.disabled = busy || !shapeSelected;
    ui.connect.disabled = busy || (!connecting && selected === undefined);
    ui.connect.textContent = connecting ? 'Cancelar conexión' : 'Conectar';
    ui.remove.disabled = busy || selected === undefined;
    canvas.classList.toggle('busy', busy);
    canvas.classList.toggle('connecting', connecting);
  }

  function renderCanvas({ board, selectedId, connectSourceId }) {
    const shapes = board.elements.filter(e => e.type !== 'CONNECTOR');
    const elementsById = new Map(board.elements.map(e => [e.id, e]));
    const nodes = [];

    // Connectors first, so shapes are drawn on top of the lines.
    for (const connector of board.elements.filter(e => e.type === 'CONNECTOR')) {
      const source = elementsById.get(connector.sourceId);
      const target = elementsById.get(connector.targetId);
      if (!source || !target) continue;
      const from = center(source);
      const to = center(target);
      const line = { x1: from.x, y1: from.y, x2: to.x, y2: to.y };
      const group = svg('g', {
        class: connector.id === selectedId ? 'connector selected' : 'connector',
        'data-id': connector.id,
      });
      group.append(svg('line', { ...line, class: 'hit' }), svg('line', { ...line, class: 'stroke' }));
      nodes.push(group);
    }

    for (const element of shapes) {
      const box = bounds(element);
      const classes = ['shape', element.type.toLowerCase()];
      if (element.id === selectedId) classes.push('selected');
      if (element.id === connectSourceId) classes.push('connect-source');
      const group = svg('g', { class: classes.join(' '), 'data-id': element.id });
      group.append(svg('rect', { ...box, rx: 8, class: 'body' }));
      const label = svg('text', { x: box.x + box.width / 2, y: box.y + box.height / 2, class: 'label' });
      label.textContent = element.text; // textContent, never innerHTML: the text comes from users
      group.append(label);
      nodes.push(group);
    }

    canvas.replaceChildren(...nodes);
  }

  // ---------------- gestures -> intents (the app decides what they mean) ----------------

  function toCanvasPoint(event) {
    return new DOMPoint(event.clientX, event.clientY).matrixTransform(canvas.getScreenCTM().inverse());
  }

  canvas.addEventListener('pointerdown', event => {
    canvas.focus({ preventScroll: true });
    if (!current || current.remote.status === 'loading') return;
    const node = event.target.closest('[data-id]');
    if (!node) {
      emit('pressBackground');
      return;
    }
    const id = node.dataset.id;
    const wasConnecting = current.mode === 'connect';
    emit('pressElement', id);
    const element = findElement(id);
    if (wasConnecting || !element || element.type === 'CONNECTOR') return;
    const pointer = toCanvasPoint(event);
    drag = { id, pointerId: event.pointerId, dx: pointer.x - element.x, dy: pointer.y - element.y };
    canvas.setPointerCapture(event.pointerId);
  });

  canvas.addEventListener('pointermove', event => {
    if (!drag || event.pointerId !== drag.pointerId) return;
    const element = findElement(drag.id);
    if (!element) return;
    const box = bounds(element);
    const pointer = toCanvasPoint(event);
    // Only the local state changes while dragging; nothing is sent until Save.
    const x = Math.round(clamp(pointer.x - drag.dx, 0, CANVAS.width - box.width));
    const y = Math.round(clamp(pointer.y - drag.dy, 0, CANVAS.height - box.height));
    emit('move', drag.id, x, y);
  });

  const endDrag = () => {
    drag = null;
  };
  canvas.addEventListener('pointerup', endDrag);
  canvas.addEventListener('pointercancel', endDrag);

  canvas.addEventListener('keydown', event => {
    if (event.key === 'Delete' || event.key === 'Backspace') {
      event.preventDefault();
      emit('removeSelected');
    } else if (event.key === 'Escape') {
      emit('cancel');
    }
  });

  const confirmDiscard = () => !current?.dirty
    || window.confirm('Hay cambios sin guardar que se perderán. ¿Desea continuar?');

  ui.create.addEventListener('click', () => {
    if (!confirmDiscard()) return;
    const name = window.prompt('Nombre del nuevo board:', 'Architecture Board');
    if (name !== null) emit('createBoard', name.trim());
  });
  ui.load.addEventListener('click', () => {
    if (confirmDiscard()) emit('loadBoard', ui.boardId.value.trim());
  });
  ui.boardId.addEventListener('keydown', event => {
    if (event.key === 'Enter') ui.load.click();
  });
  ui.name.addEventListener('change', () => {
    emit('rename', ui.name.value.trim());
    ui.name.value = current.board.name; // shows the accepted name (or restores the old one)
  });
  ui.save.addEventListener('click', () => emit('save'));
  ui.retry.addEventListener('click', () => emit('retry'));
  ui.addRect.addEventListener('click', () => emit('addRectangle'));
  ui.addText.addEventListener('click', () => emit('addText'));
  ui.connect.addEventListener('click', () => emit('toggleConnect'));
  ui.remove.addEventListener('click', () => emit('removeSelected'));
  ui.editText.addEventListener('click', () => {
    const selected = findElement(current?.selectedId);
    if (!selected) return;
    const text = window.prompt('Nuevo texto del elemento:', selected.text);
    if (text !== null) emit('editText', selected.id, text);
  });

  return {
    render,
    on(next) {
      handlers = { ...handlers, ...next };
    },
    /** Board id kept in the URL (#id), so reloading the page brings the same board back. */
    initialBoardId: boardIdFromUrl,
  };
}
