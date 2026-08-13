# MagicTickets

Core de dominio en Java puro para la gestión de compra de tickets a eventos, estructurado bajo **Arquitectura Limpia (Clean Architecture)** y **Domain-Driven Design (DDD)**. El sistema valida tres reglas de negocio antes de autorizar una compra (cantidad solicitada válida, límite máximo de tickets por usuario y disponibilidad de stock), protege la creación de eventos con reglas de negocio auto-validadas, y desacopla completamente la lógica de negocio de cualquier mecanismo de persistencia mediante el patrón Repositorio.

Proyecto correspondiente al **Hito 3** del módulo "Fundamentos de Calidad y TDD en Java" — Unidad 3: Arquitectura Limpia y Diseño Guiado por el Dominio (DDD). Evoluciona directamente sobre el dominio entregado en el **Hito 1** (`hito-1-entrega`), sin introducir un stack tecnológico nuevo.

---

## Índice

1. [Arquitectura](#arquitectura)
2. [Estructura del repositorio](#estructura-del-repositorio)
3. [Modelo de dominio](#modelo-de-dominio)
4. [Patrón Repositorio](#patrón-repositorio)
5. [Reglas de negocio y decisiones de diseño](#reglas-de-negocio-y-decisiones-de-diseño)
6. [Suite de tests](#suite-de-tests)
7. [Comandos de ejecución](#comandos-de-ejecución)
8. [Evidencia de cobertura](#evidencia-de-cobertura)

---

## Arquitectura

Este proyecto implementa **Clean Architecture** en tres capas concéntricas, siguiendo la Regla de Oro: las capas externas pueden depender de las internas, pero las internas jamás conocen ni referencian a las externas.

- **Domain (`domain/`):** el núcleo absoluto. Entidades, Value Objects, excepciones de negocio, contratos de repositorio y servicios de dominio. Java puro — cero anotaciones de frameworks (`@Service`, `@Autowired`, `@Entity`), cero dependencias de persistencia o infraestructura.
- **Application (`application/`):** orquesta los Casos de Uso e incluye los Puertos (interfaces hacia el exterior, como notificaciones). Depende únicamente de contratos abstractos del dominio.
- **Infrastructure (`infrastructure/`):** el anillo más externo. Aloja las implementaciones concretas — en este hito, un repositorio en memoria, sin base de datos real.

**Cero acoplamiento tecnológico en el núcleo:** ningún archivo de `domain/` ni `application/` importa Spring, JPA, ni ningún framework externo. La suite de tests corre íntegramente en memoria, sin necesidad de una base de datos real.

**Inyección por constructor, sin excepción:** `TicketPurchaseService` depende de tres abstracciones — `EventRepository`, `PurchaseNotifier`, `PurchaseValidator` — recibidas todas por constructor. En ningún punto del caso de uso se instancia una implementación concreta con `new`.

## Estructura del repositorio

```
magictickets/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/java/com/magictickets/
    │   ├── domain/                          # CAPA DE DOMINIO (Java puro)
    │   │   ├── entity/
    │   │   │   ├── Event.java
    │   │   │   ├── ShowStatus.java
    │   │   │   └── ShowCategory.java
    │   │   ├── valueobject/
    │   │   │   └── EventDate.java
    │   │   ├── exception/
    │   │   │   ├── InvalidQuantityException.java
    │   │   │   ├── MaxTicketsExceededException.java
    │   │   │   ├── OutOfStockException.java
    │   │   │   ├── InvalidEventDateException.java
    │   │   │   └── EventNotFoundException.java
    │   │   ├── repository/
    │   │   │   └── EventRepository.java     # Contrato — solo interfaz
    │   │   └── service/
    │   │       └── PurchaseValidator.java   # Servicio de dominio
    │   │
    │   ├── application/                     # CAPA DE APLICACIÓN
    │   │   ├── usecase/
    │   │   │   └── TicketPurchaseService.java
    │   │   └── port/
    │   │       └── PurchaseNotifier.java    # Puerto hacia el exterior
    │   │
    │   └── infrastructure/                  # CAPA DE INFRAESTRUCTURA
    │       └── persistence/
    │           └── InMemoryEventRepository.java
    │
    └── test/java/com/magictickets/
        ├── domain/
        │   ├── entity/EventTest.java
        │   ├── valueobject/EventDateTest.java
        │   └── service/PurchaseValidatorTest.java
        ├── application/usecase/TicketPurchaseServiceTest.java
        └── infrastructure/persistence/InMemoryEventRepositoryTest.java
```

**Por qué esta organización:**

- La estructura replica fielmente el patrón de referencia provisto para el Hito 3: `domain` (entidad, valueobject, exception, repository, service), `application` (usecase, port), `infrastructure` (persistence).
- `domain/service/PurchaseValidator` aloja las tres validaciones de negocio (cantidad, máximo de tickets, stock) que en el Hito 1 vivían como métodos privados dentro del caso de uso — se extrajeron a un Servicio de Dominio independiente, con métodos públicos, siguiendo el patrón mostrado para servicios de dominio.
- `application/port/PurchaseNotifier` se reubicó desde `domain/` (Hito 1) a `application/port/`, reconociendo que un puerto de notificación es parte de la orquestación de casos de uso, no del núcleo de reglas de negocio puras.
- `src/test` es un espejo exacto de `src/main`, misma convención que en Hito 1.

## Modelo de dominio

### `Event` (Entidad)

Representa el evento sobre el que se realiza la compra. Identidad única mediante `id` (`String`, generado automáticamente vía `UUID` en el constructor), que trasciende cualquier cambio en sus atributos.

- `id` (`private final String`): identidad de la Entidad, autogenerada, inmutable.
- `name` (`private final String`): inmutable una vez creado el evento.
- `stock` (`private int`): mutable, se reduce con cada compra válida mediante `reduceStock(int quantity)`, sin validación interna (responsabilidad delegada a `PurchaseValidator`).
- `date` (`private final EventDate`): Value Object, ver detalle abajo.
- `status` (`private ShowStatus`): nace siempre en `SCHEDULED`; no se expone constructor que permita crear un evento en otro estado.
- `category` (`private final ShowCategory`): validada como no nula en el constructor (`IllegalArgumentException` — validación técnica, no de negocio, por lo que no amerita una excepción de dominio propia).

### `EventDate` (Value Object)

```java
public record EventDate(LocalDate value) {
    public EventDate {
        if (value == null) {
            throw new InvalidEventDateException("Event date cannot be null");
        }
        if (!value.isAfter(LocalDate.now())) {
            throw new InvalidEventDateException(
                "Event date must be strictly after today: " + value + " is invalid");
        }
    }
}
```

Implementado con `record` de Java: inmutable por diseño del lenguaje, con `equals()`/`hashCode()`/`toString()` generados automáticamente. Auto-valida en su constructor compacto que la fecha no sea nula y que sea estrictamente posterior a hoy — un evento no puede crearse para el mismo día ni para el pasado, dado que el costo operativo de administrar un evento con un único día de venta de entradas no compensa la ganancia esperada.

### `ShowStatus` y `ShowCategory` (enums)

Modelan estados y categorías cerradas del negocio. No se implementan como `record` porque un `enum` de Java ya es inmutable y auto-validado por el propio compilador — no admite valores fuera de sus constantes declaradas, por lo que no requiere lógica de validación adicional.

### `PurchaseValidator` (Servicio de Dominio)

Concentra las tres reglas de validación de una compra, previamente alojadas como métodos privados de `TicketPurchaseService`:

```java
public void validateQuantity(int quantity) { ... }
public void validateMaxTickets(int quantity) { ... }
public void validateStock(Event event, int quantity) { ... }
```

Cada método lanza la excepción de dominio correspondiente ante una regla violada. Al ser una clase de dominio sin dependencias externas, no requiere inyección de nada en su propio constructor.

### `EventRepository` (contrato de Repositorio)

```java
public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
}
```

Interfaz pura, definida en `domain/repository/`, sin heredar de ningún framework de persistencia (`JpaRepository` o similar). Opera exclusivamente con el objeto de dominio `Event`. Su única implementación en este hito es `InMemoryEventRepository` (infraestructura), respaldada por un `HashMap` — sin base de datos real.

### `PurchaseNotifier` (Puerto)

Sin cambios de comportamiento respecto al Hito 1; reubicado a `application/port/`. Representa la única dependencia de comunicación externa del sistema.

### `TicketPurchaseService` (Caso de Uso)

```java
public TicketPurchaseService(EventRepository eventRepository, PurchaseNotifier notifier, PurchaseValidator validator) {
    this.eventRepository = eventRepository;
    this.notifier = notifier;
    this.validator = validator;
}

public void purchase(String eventId, int quantity) {
    Event event = findEvent(eventId);
    validator.validateQuantity(quantity);
    validator.validateMaxTickets(quantity);
    validator.validateStock(event, quantity);
    event.reduceStock(quantity);
    eventRepository.save(event);
    notifier.notifyPurchase(event.getName(), quantity);
}
```

Cambio de diseño respecto al Hito 1: el método `purchase` ya no recibe el objeto `Event` directamente — recibe únicamente su `eventId`, y es el propio caso de uso quien lo busca a través del repositorio inyectado (`eventRepository.findById(eventId)`), lanzando `EventNotFoundException` si no existe. Esto demuestra el desacoplamiento real del patrón Repositorio: quien invoca el caso de uso no necesita tener el objeto de dominio en memoria, solo su identificador.

## Patrón Repositorio

**El problema que resuelve:** antes de este hito, el dominio no contemplaba persistencia — un `Event` vivía únicamente en memoria durante la ejecución de un test o llamada. El patrón Repositorio introduce la abstracción necesaria para que el Caso de Uso pueda almacenar y recuperar un `Event` sin acoplarse a un mecanismo de almacenamiento específico.

**Anatomía aplicada:**
- **Contrato** (`domain/repository/EventRepository.java`): define *qué* se puede hacer, sin saber *cómo*.
- **Implementación** (`infrastructure/persistence/InMemoryEventRepository.java`): resuelve el *cómo*, hoy con un `HashMap` en memoria.

**Impacto ante un cambio futuro:** si se reemplaza el mecanismo de almacenamiento (por ejemplo, una base de datos real), el dominio y la aplicación permanecen intactos. Solo se reemplaza `InMemoryEventRepository` por una nueva implementación que cumpla el mismo contrato `EventRepository`.

## Reglas de negocio y decisiones de diseño

| # | Regla | Excepción | Condición de rechazo |
|---|-------|-----------|----------------------|
| 1 | La cantidad solicitada debe ser positiva | `InvalidQuantityException` | `quantity <= 0` |
| 2 | Máximo 5 tickets por compra/usuario | `MaxTicketsExceededException` | `quantity > 5` |
| 3 | Debe existir stock suficiente | `OutOfStockException` | `quantity > event.getStock()` |
| 4 | La fecha del evento no puede ser nula ni de hoy/pasada | `InvalidEventDateException` | `date == null \|\| !date.isAfter(today)` |
| 5 | El evento debe existir para poder comprarlo | `EventNotFoundException` | `repository.findById(id)` vacío |
| 6 | La categoría del evento no puede ser nula | `IllegalArgumentException` | `category == null` (validación técnica, no excepción de dominio) |

**Por qué `category` usa `IllegalArgumentException` y no una excepción de dominio propia:** las excepciones de dominio existentes (`InvalidQuantityException`, `MaxTicketsExceededException`, etc.) protegen reglas específicas del negocio de MagicTickets, cada una con una justificación de negocio propia. Un `null`-check de un parámetro obligatorio es una garantía técnica genérica, no una regla de negocio — se usa el tipo estándar de Java reservado para ese caso, evitando diluir la semántica de las excepciones de dominio reales.

**Por qué `PurchaseValidator` es un Servicio de Dominio y no queda dentro del Caso de Uso:** las tres validaciones no dependen de ningún estado propio del caso de uso (repositorio, notificador) — son lógica de negocio pura, reutilizable independientemente de quién orqueste la compra. Separarlas permite testearlas de forma aislada, sin necesidad de mocks, y sigue el patrón mostrado en el material de referencia del hito para servicios de dominio.

**Continuidad con Hito 2 (frontend TypeScript):** los atributos `date`, `status` (`ShowStatus`) y `category` (`ShowCategory`) —ya modelados en el frontend desde el Hito 2— se incorporan al dominio Java en este hito, cerrando la brecha de continuidad entre ambos stacks del proyecto, con base en un Glosario de Lenguaje Ubicuo elaborado previamente.

## Suite de tests

Framework: **JUnit 5** + **Mockito Core**. Patrón **AAA** en los 27 tests.

| Clase de test | Qué cubre |
|---|---|
| `TicketPurchaseServiceTest` (9 tests) | Orquestación completa del caso de uso, con `EventRepository` y `PurchaseNotifier` mockeados |
| `EventTest` (5 tests) | Identidad, validación de categoría, estado inicial `SCHEDULED` |
| `EventDateTest` (5 tests) | Auto-validación del Value Object: nulo, hoy, pasado, futuro válido |
| `PurchaseValidatorTest` (6 tests) | Las tres reglas de validación, aisladas del caso de uso, sin mocks |
| `InMemoryEventRepositoryTest` (2 tests) | Implementación real del repositorio: guardar/buscar, id inexistente |

**Por qué `PurchaseValidator` se usa real (sin mock) en `TicketPurchaseServiceTest`:** a diferencia de `EventRepository` y `PurchaseNotifier` (dependencias externas al dominio: persistencia y comunicación), `PurchaseValidator` es lógica de negocio pura sin efectos secundarios. Mockearlo ocultaría el comportamiento que los tests buscan demostrar.

**Por qué existe `InMemoryEventRepositoryTest` además de los mocks en `TicketPurchaseServiceTest`:** los mocks verifican que el caso de uso *usa correctamente* la interfaz del repositorio; este test verifica que la implementación real *efectivamente funciona*. Ambos son necesarios y no redundantes.

## Comandos de ejecución

Compilar y verificar el proyecto:

```bash
mvn clean compile
```

Ejecutar la suite de tests:

```bash
mvn test
```

Generar el reporte de cobertura:

```bash
mvn jacoco:report
```

## Evidencia de cobertura

El reporte HTML se genera en:

```
target/site/jacoco/index.html
```

Según el reporte HTML oficial de JaCoCo (fuente de verdad definida por la guía de evaluación), todos los paquetes con contenido ejecutable muestran:

- **Instructions:** 100% (0 de 283 sin cubrir)
- **Branches:** 100% (0 de 14 sin cubrir)
- **Methods:** 100% (0 de 26 sin cubrir)
- **Lines:** 100% (0 de 78 sin cubrir)

**Nota sobre una discrepancia observada:** el plugin adicional `jacoco-console-reporter` reporta cobertura de clases inferior a 100% en consola. Esto ocurre porque contabiliza las interfaces sin cuerpo (`EventRepository`, `PurchaseNotifier`) como clases independientes sin instrucciones ejecutables, mientras que el reporte HTML oficial de JaCoCo no las computa de la misma forma — mismo comportamiento ya documentado en el Hito 1. Se prioriza el reporte HTML por ser la fuente de verdad explícita de la guía de evaluación.

---

## Continuidad del proyecto

Este repositorio se reutiliza directamente desde el Hito 1, trabajando sobre `main`, sin repositorio nuevo ni rama separada — Hito 3 es una refactorización arquitectónica del mismo dominio Java, no un módulo de stack distinto. El estado exacto del código al cierre del Hito 1 permanece accesible mediante el tag `hito-1-entrega`.
