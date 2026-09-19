# ARSW Collaborative Architecture Board — G05

Aplicación incremental del curso Arquitecturas de Software (ECI, 2026-2). Un Board es un plano con elementos
(RECTANGLE, TEXT y CONNECTOR) que se crea, edita y guarda mediante una API REST y un cliente web SVG.

| Laboratorio | Estado | Qué agrega |
|---|---|---|
| Lab 04 — Architecture Foundation | Entregado | Backend modular: controller → application service → repository port → adaptador en memoria |
| Lab 05 — Interactive Board | Entregado | Cliente web SVG (ES Modules) sobre la misma API y el elemento CONNECTOR |
| Lab 06 — Real-Time Collaboration | Pendiente | WebSocket/STOMP con un tópico por board |

## Requisitos

- Java 21 y Maven 3.9 o superior
- Un navegador actual (Chrome, Edge o Firefox)

## Ejecutar

```bash
mvn spring-boot:run
```

Abrir <http://localhost:8080/>. Después de cambiar archivos de `src/main/resources/static`, reinicie la
aplicación (o recompile en el IDE) y recargue con Ctrl+F5.

## Probar

```bash
mvn test
```

Incluye pruebas de dominio, de casos de uso, del contrato REST con MockMvc y una prueba de integración
que guarda y recarga un board con conectores.

## Uso del cliente web

1. **Nuevo board** pide un nombre, crea el board (POST) y muestra su `boardId`, que también queda en la URL (`#id`).
2. **Cargar** trae un board existente por su `boardId` (GET). Recargar la página (F5) vuelve a cargar el board de la URL.
3. **+ Rectángulo** y **+ Texto** agregan elementos; se seleccionan con un clic y se mueven arrastrándolos.
4. **Editar texto** cambia la etiqueta del elemento seleccionado.
5. **Conectar**: seleccione el elemento origen, pulse Conectar y haga clic en el destino (Esc cancela).
6. **Eliminar** (o la tecla Supr) borra el elemento seleccionado y sus conectores.
7. **Guardar** envía el board completo (PUT). Si una operación remota falla aparece **Reintentar**.

Los cambios son locales hasta pulsar Guardar; la parte superior indica `idle`, `loading`, `success` o `error`.

## API

| Método | Recurso | Respuesta |
|---|---|---|
| POST | `/api/boards` | `201` + `Location` + Board |
| GET | `/api/boards/{boardId}` | `200` + Board |
| PUT | `/api/boards/{boardId}` | `200` + Board actualizado |

Contrato completo, reglas de CONNECTOR y códigos de error: [docs/api-contract.md](docs/api-contract.md).

## Estructura

```text
src/main/java/edu/eci/arsw/collabboard
├── domain/model                  Board, BoardElement, ElementType (sin dependencias de framework)
├── application/port/out          BoardRepository (puerto de salida)
├── application/service           BoardApplicationService (casos de uso)
└── infrastructure
    ├── persistence               InMemoryBoardRepository (adaptador)
    └── web/rest                  BoardRestController, GlobalExceptionHandler, DTOs
src/main/resources/static
├── index.html, css/app.css
└── js
    ├── app.js                    BoardApp: orquestación
    ├── api/board-api-client.js   único módulo con fetch
    ├── state/board-state.js      estado local (fuente de verdad)
    └── ui/board-view.js          render SVG y eventos
```

## Documentación

- [ADR-001 — Repository boundary](docs/ADR-001-repository-boundary.md)
- [ADR-002 — Client boundaries](docs/ADR-002-client-boundaries.md)
- [Contrato REST](docs/api-contract.md)
- [Evidencia de arquitectura](docs/architecture/README.md)
- [Declaración de uso de IA](docs/AI_USAGE.md)
