# ADR-001 — Repository Boundary

## Status
Accepted


## Context
El backend del Collaborative Board debe almacenar agregados `Board`. En el Lab 04
el único almacenamiento es un mapa en memoria del proceso, pero los laboratorios
siguientes (Lab 07+) introducen estado concurrente y una estrategia de
persistencia real. Los casos de uso (`create`, `get`, `replace`) no deben
reescribirse cada vez que cambie la tecnología de almacenamiento, y deben poder
probarse sin levantar el servidor web ni una base de datos.
La tensión de diseño: ¿dónde vive el contrato de persistencia y quién puede
depender de quién?

## Decision
El contrato de persistencia es un puerto de salida, `BoardRepository`,
ubicado en `application/port/out`: lo posee la capa de aplicación, no la
infraestructura.
El puerto tiene forma de dominio: `save`, `findById`, `existsById`. Sin
lenguaje de consulta, paginación, transacciones ni tipos de framework.
`InMemoryBoardRepository` vive en `infrastructure/persistence` e implementa
el puerto. Es la única clase que sabe que el almacenamiento es un `HashMap`.
`BoardApplicationService` depende de `BoardRepository` (la abstracción) y se
ensambla por inyección de constructor. Nunca nombra al adaptador concreto.
`BoardRestController` depende solo de `BoardApplicationService`; no tiene estado
ni accede al almacenamiento.
Es el Principio de Inversión de Dependencias: la llamada fluye hacia afuera
(`controller → service → adapter → map`), pero la dependencia de código del
adaptador apunta hacia adentro, al puerto definido por la aplicación.


## Positive consequences
Modificabilidad: reemplazar `InMemoryBoardRepository` por otro adaptador
(JPA, Redis, mapa concurrente) no exige cambios en el dominio, el servicio ni el
controlador; solo una clase nueva que implemente `BoardRepository` y su registro
como bean.
Testabilidad: `BoardApplicationServiceTest` ejercita todos los casos de uso
con el adaptador en memoria y sin contexto de Spring; un fake/mock del puerto
funcionaría igual.
Sin fuga de infraestructura al dominio: el paquete `domain` no importa nada
de `infrastructure` ni de Spring.
Errores uniformes: las excepciones internas (`BoardNotFoundException`,
`IllegalArgumentException`) se traducen en un único lugar al contrato
`ApiError`.


## Trade-off
Una indirección extra (interfaz + adaptador) para lo que hoy es un único
`HashMap`. Para este tamaño es "ceremonia" que solo rinde en labs posteriores.
`existsById` es una pequeña concesión de claridad/eficiencia para el caso de uso
`replace`; un puerto estrictamente mínimo expondría solo `save` y `findById`.
El puerto se mantiene pequeño de forma deliberada; es fácil dejar que derive
hacia una interfaz con forma de base de datos.


## Evidence / validation
`mvn test` — 12 pruebas verdes (6 de casos de uso, 6 de contrato REST); las
pruebas del servicio no levantan el servidor web.
Corrida HTTP manual: `POST` → 201 + `Location`; `GET` existente → 200; `GET`
inexistente → 404 `ApiError`; `PUT` existente → 200 mismo id; `PUT` inexistente
→ 404; `name` en blanco → 400; JSON malformado → 400.
Grep: no hay `import ...infrastructure...` dentro de `domain/`;
`BoardApplicationService` referencia solo a `BoardRepository`, nunca a
`InMemoryBoardRepository`.
El diagrama de clases y la vista ArchiMate de este documento usan esos mismos
nombres y direcciones de dependencia.

