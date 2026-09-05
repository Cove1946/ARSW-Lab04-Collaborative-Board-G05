# AI Usage Declaration

Declaring AI use does not reduce the grade. We can explain and validate every
submitted decision.

## Cómo se usó la IA

El apoyo de IA (Claude Code) se limitó a dos roles: **redacción** de bloques de
código propuestos para cada punto de la sección 6, y **opinión/revisión** de esos
bloques y de los documentos de arquitectura. La decisión de qué aceptar, ajustar
o rechazar fue siempre del equipo, verificada con `mvn test` y pruebas manuales
por HTTP antes de incorporar cualquier cambio.

| Tool | Activity | Prompt / purpose | How we validated the result | What we changed / rejected |
|---|---|---|---|---|
| Claude Code (Claude Sonnet) | Análisis inicial: enunciado del Lab 04 + diapositivas de Semana 4 (multicapa, DIP, DI, monolito modular) contra el código starter | Se pidió comparar los requisitos obligatorios (RA-01..RA-07) y los criterios de aceptación con lo ya provisto en el starter, para saber exactamente qué faltaba por completar en cada archivo | Revisamos nosotros mismos cada requisito contra el código fuente antes de empezar a implementar; no se tomó el análisis como verdad sin contraste | — |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.1 Dominio** (`Board`, `BoardElement`) | Se pidió redactar cómo documentar las invariantes mínimas (id/nombre no vacíos, tipos soportados) y opinar si convenía añadir una invariante extra (ids de elemento únicos por board) | Se leyó el `record` resultante, se corrió `mvn compile` y se revisó que no metiera lógica HTTP ni de persistencia en el dominio | Se aceptó tal cual, incluida la invariante de ids de elemento únicos. Se transcribió a mano al IDE (no se copió el archivo completo), por lo que el formato final difiere en detalles menores (orden de imports, una línea en blanco extra antes de la clase) sin cambio funcional. Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.2 Puerto** (`BoardRepository`) | Se pidió opinar si las 3 operaciones (`save`, `findById`, `existsById`) eran el mínimo necesario o si sobraba/faltaba alguna, evitando copiar la forma de un framework de BD | Se contrastó cada operación contra los 3 casos de uso reales (create/get/replace) antes de dejarla | Se aceptaron las 3 operaciones y la documentación tal cual, sin quitar ni agregar métodos. Transcripción manual con reordenamiento de imports por el IDE. Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.3 Adapter** (`InMemoryBoardRepository`) | Se pidió redactar la documentación de la semántica de `save` (upsert) y opinar si hacía falta copia defensiva dado que el modelo ya es inmutable | Se verificó manualmente que `Board`/`BoardElement` son records inmutables antes de aceptar "sin copia defensiva" | Se aceptó la decisión de no hacer copia defensiva y el `HashMap` simple sin sincronización (concurrencia fuera de alcance). Transcripción manual introdujo un typo cosmético en un comentario (`///` en vez de `//`), corregible sin afectar el código. Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.4 Application Service** (`BoardApplicationService`) | Se pidió redactar la implementación de `createBoard`/`getBoard`/`replaceBoard` contra el puerto, y opinar sobre casos borde (replace de board inexistente no debe crear) | Se corrió `mvn test` (`BoardApplicationServiceTest`) tras aplicar el bloque; luego se pidió **revertirlo deliberadamente** para confirmar que los 6 tests de servicio fallaban sin la implementación (control de que las pruebas no eran falsos positivos) — fallaron los 6 como se esperaba — y se volvió a aplicar después | Se aceptó la lógica completa (UUID generado en el servidor, `BoardNotFoundException` en get/replace, replace nunca crea). Se transcribió manualmente, lo que dejó una indentación irregular en una línea de `replaceBoard` (cosmético, sin efecto en el comportamiento ni en `mvn test`). Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.5 Controlador REST** (`BoardRestController`) | Se pidió opinar si el controlador seguía siendo "delgado" (RA-01/RA-07) y si el header `Location` en el `201` era una mejora justificable dentro del alcance | Se probó cada endpoint por HTTP con `curl` (POST/GET/PUT) tras aplicar el cambio | Se aceptó tal cual, sin modificaciones: mismo código, mismo header `Location`, mismos imports. Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.6 Manejo de errores** (`GlobalExceptionHandler`) | Se pidió opinar sobre qué excepciones adicionales cubrir (JSON malformado, fallback genérico) sin exponer stack traces ni mensajes internos de Java | Se probaron los casos de error por HTTP (body inválido, JSON malformado, board inexistente) verificando la forma exacta de `ApiError` en cada respuesta | Se aceptó tal cual: se retiró el handler de `UnsupportedOperationException` del starter y se agregaron `MALFORMED_REQUEST` y el fallback `INTERNAL_ERROR`. Sin modificaciones ni rechazos. |
| Claude Code (Claude Sonnet) | Redacción de propuesta para **6.7 Pruebas** (`BoardApplicationServiceTest`, `BoardRestControllerTest`) | Se pidió redactar casos de prueba que cubrieran el caso obligatorio "Board inexistente" tanto a nivel de servicio como de contrato REST | Se ejecutó `mvn test` y se leyó el reporte de Surefire para confirmar qué pasaba y qué fallaba en cada iteración (12 pruebas, 6+6) | Se aceptaron los 12 casos propuestos sin cambios, incluida la anotación `@MockitoBean` (reemplazo de `@MockBean` en Spring Boot 3.4+). Se usaron activamente para validar 6.4: fallaron antes de implementarlo, pasaron después. Nada rechazado. |
| Claude Code (Claude Sonnet) | Redacción y opinión sobre los documentos `docs/api-contract.md`, `docs/ADR-001-repository-boundary.md`, `docs/Arquitectura.md` (diagramas PlantUML/Mermaid) y `docs/Solucion_Implementacion.md` | Se pidió redactar el contrato REST real documentado, el ADR con el formato de la plantilla (Contexto/Decisión/Consecuencias/Trade-off/Evidencia), y una propuesta de diagramas de clases y vista ArchiMate coherentes con el código | Se comparó cada nombre de clase y cada flecha de dependencia del diagrama contra el código fuente real, para evitar diagramas "ideales" que no correspondieran a lo entregado | Se aceptó el contrato REST y el ADR como base, usados para completar los archivos entregables. Los diagramas PlantUML/Mermaid se tomaron como borrador: el equipo los pasa a Draw.io manualmente en vez de usar el render directo, por preferencia de herramienta, no por desacuerdo con el contenido. Nada rechazado de fondo. |

## Decisiones que el equipo asume y puede defender

- Por qué el puerto (`BoardRepository`) vive en `application/port/out` y el
  adaptador concreto en `infrastructure/persistence` — no al revés.
- Por qué `BoardApplicationService` nunca importa ni referencia
  `InMemoryBoardRepository` directamente (DIP).
- El contrato de errores completo (`ApiError`) y por qué se eligió cada código
  HTTP (`BOARD_NOT_FOUND`, `INVALID_REQUEST`, `INVALID_INPUT`,
  `MALFORMED_REQUEST`).
- Las invariantes de dominio en `Board` / `BoardElement` y por qué se
  consideraron el mínimo necesario (ni más ni menos) para esta fase del
  laboratorio.
- Qué cambiaría y qué no si `InMemoryBoardRepository` se reemplazara por otro
  adaptador (pregunta de sustentación del enunciado).