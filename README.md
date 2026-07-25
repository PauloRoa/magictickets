# MagicTickets

Core de dominio puro para la gestión de compra de tickets a eventos. El sistema valida tres reglas de negocio antes de autorizar una compra (cantidad solicitada válida, límite máximo de tickets por usuario y disponibilidad de stock) y notifica la compra a través de una dependencia externa inyectada por constructor.

Proyecto correspondiente al **Hito 1** del módulo "Fundamentos de Calidad y TDD en Java".

---

## Índice

1. [Arquitectura](#arquitectura)
2. [Estructura del repositorio](#estructura-del-repositorio)
3. [Modelo de dominio](#modelo-de-dominio)
4. [Reglas de negocio y decisiones de diseño](#reglas-de-negocio-y-decisiones-de-diseño)
5. [Suite de tests](#suite-de-tests)
6. [Comandos de ejecución](#comandos-de-ejecución)
7. [Evidencia de cobertura](#evidencia-de-cobertura)

---

## Arquitectura

Este proyecto implementa un **Core de Entidades Puro** (Clean Architecture / Puertos y Adaptadores). El paquete `domain` no depende de frameworks, bases de datos ni infraestructura externa:

- **Cero anotaciones de framework**: no se usa `@Service`, `@Autowired`, `@Entity` ni ninguna dependencia de Spring o de persistencia.
- **Java puro**: toda la lógica de negocio es evaluable sin levantar ningún contenedor o servidor.
- **Inyección por constructor**: `TicketPurchaseService` depende de la interfaz `PurchaseNotifier` (el puerto), no de una implementación concreta. La dependencia se recibe en el constructor, nunca se instancia dentro de la clase.

## Estructura del repositorio

```
magictickets/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/java/com/magictickets/domain/
    │   ├── Event.java
    │   ├── PurchaseNotifier.java
    │   ├── TicketPurchaseService.java
    │   └── exception/
    │       ├── InvalidQuantityException.java
    │       ├── MaxTicketsExceededException.java
    │       └── OutOfStockException.java
    └── test/java/com/magictickets/domain/
        └── TicketPurchaseServiceTest.java
```

**Por qué esta organización:**

- `domain` concentra las entidades, el servicio y el puerto (`PurchaseNotifier`). `domain/exception` separa las excepciones de negocio del resto, siguiendo la misma convención que el material de referencia de la sesión.
- La estructura de `src/test` es un espejo exacto de `src/main`, convención estándar de Maven que facilita ubicar el test correspondiente a cada clase de producción.
- Un único archivo de test (`TicketPurchaseServiceTest`) concentra todos los casos de `TicketPurchaseService`. `PurchaseNotifier` no tiene archivo de test propio porque es una interfaz sin lógica: no hay comportamiento que probar de forma aislada, solo su uso a través de `TicketPurchaseService` (verificado con Mockito).

## Modelo de dominio

### `Event`

Representa el evento sobre el que se realiza la compra.

- `name` (`private final String`): inmutable una vez creado el evento. Se expone mediante `getName()`, necesario para que `TicketPurchaseService` pueda incluir el nombre del evento al notificar la compra.
- `stock` (`private int`): mutable, se reduce con cada compra válida.
- Ambos atributos son `private` (encapsulamiento): ningún código externo puede modificar el stock directamente; solo puede hacerlo a través de `reduceStock(int quantity)`.
- `reduceStock()` **no contiene validación alguna**. Es una decisión deliberada: la responsabilidad de decidir si una reducción de stock es válida no le corresponde a `Event`, sino al servicio que orquesta la compra (`TicketPurchaseService`). Esto evita duplicar reglas de negocio en dos lugares distintos.

### `PurchaseNotifier` (puerto)

```java
public interface PurchaseNotifier {
    void notifyPurchase(String eventName, int quantity);
}
```

Representa la única dependencia externa real del dominio: notificar que una compra se completó (por ejemplo, hacia un sistema de correo o SMS). Se modela como interfaz — el dominio depende de la abstracción, no de una implementación concreta, siguiendo el patrón de Puertos y Adaptadores. La implementación real de este puerto (envío efectivo de la notificación) es infraestructura y queda fuera del alcance de este hito.

### `TicketPurchaseService`

Orquesta la operación de compra (`purchase(Event event, int quantity)`), delegando cada validación a un método privado con responsabilidad única, y notificando al finalizar:

```java
public TicketPurchaseService(PurchaseNotifier notifier) {
    this.notifier = notifier;
}

public void purchase(Event event, int quantity) {
    validateQuantity(quantity);
    validateMaxTickets(quantity);
    validateStock(event, quantity);
    event.reduceStock(quantity);
    notifier.notifyPurchase(event.getName(), quantity);
}
```

Los tres métodos de validación son `private`: son detalles internos de cómo se valida una compra, no operaciones que otra clase deba invocar de forma independiente. `notifier` es `private final`, asignado una única vez en el constructor.

## Reglas de negocio y decisiones de diseño

| # | Regla | Excepción | Condición de rechazo |
|---|-------|-----------|----------------------|
| 1 | La cantidad solicitada debe ser positiva | `InvalidQuantityException` | `quantity <= 0` |
| 2 | Máximo 5 tickets por compra/usuario | `MaxTicketsExceededException` | `quantity > 5` |
| 3 | Debe existir stock suficiente | `OutOfStockException` | `quantity > event.getStock()` |

**Orden de validación (y por qué):** cantidad válida → límite máximo → stock disponible. Se valida primero lo más básico (¿es un número con sentido?) antes de evaluar reglas de negocio más específicas, y la consulta de stock —la más costosa en un sistema real, al depender de una fuente de datos— se deja al final. La notificación ocurre solo si las tres validaciones pasan y el stock ya fue reducido.

**Por qué el límite es una constante y no un atributo de `Event`:** se evaluó modelar el límite de 5 tickets como un atributo configurable por evento (`maxTicketsPerPurchase`). Se descartó porque el Hito 1 pide una regla de negocio simple y probada, no un sistema de configuración por cliente (principio YAGNI), y porque el límite de compra es una regla de la *operación*, no una característica intrínseca del evento.

**Por qué `PurchaseNotifier` sí se modela como interfaz inyectada, a diferencia del resto del dominio:** es la única dependencia externa real del sistema — una notificación implica comunicación hacia afuera del dominio (correo, SMS, cola de mensajes). El resto de la lógica (`Event`, las tres validaciones) no tiene ninguna dependencia externa, por lo que no se introdujeron interfaces adicionales sin propósito.

**Por qué no hay `if/else` anidados en las validaciones:** las tres usan `throw`, que corta la ejecución del método de inmediato. Encadenar `if` sucesivos (sin `else`) es equivalente en comportamiento a anidarlos, pero evita el anti-patrón conocido como *arrow code*.

**Idioma:** nombres de clases, métodos, variables y mensajes de excepción están en inglés, conforme al Pilar 1 de la rúbrica de evaluación.

## Suite de tests

Framework: **JUnit 5** (`junit-jupiter`) + **Mockito Core**. Patrón **AAA** (Arrange, Act, Assert) aplicado en los 7 tests, con comentarios explícitos separando cada fase.

`PurchaseNotifier` se simula con `Mockito.mock()` en los 7 tests, ya que es la única dependencia externa de `TicketPurchaseService`.

| Test | Qué valida |
|------|------------|
| `testPurchaseWithZeroQuantity` | `quantity = 0` → `InvalidQuantityException` |
| `testPurchaseWithNegativeQuantity` | `quantity = -1` → `InvalidQuantityException` |
| `testPurchaseExceedingMaxTickets` | `quantity = 7` → `MaxTicketsExceededException` |
| `testPurchaseWithExactlyMaxAllowedTickets` | `quantity = 5` (límite exacto) → **no** lanza excepción (`assertDoesNotThrow`) |
| `testPurchaseWithInsufficientStock` | Stock de 3, se piden 5 → `OutOfStockException` |
| `testPurchaseReducesStockCorrectly` | Camino feliz: compra válida reduce el stock correctamente (`assertEquals`) |
| `testPurchaseNotifiesOnSuccess` | Tras una compra exitosa, `notifier.notifyPurchase("Concierto", 3)` fue invocado (`Mockito.verify`) |

**Por qué existe el test del límite exacto (5):** confirma que la condición de rechazo es `quantity > 5` y no `quantity >= 5` — sin este test, un error de un solo carácter pasaría inadvertido pese a que el resto de la suite siga en verde.

**Por qué existe `testPurchaseNotifiesOnSuccess`:** es el único test que ejerce realmente Mockito con `verify()`, confirmando que la interacción con la dependencia externa (`PurchaseNotifier`) ocurrió con los parámetros esperados — no basta con instanciar el mock en los otros tests, hay que verificar al menos una interacción real con él.

## Comandos de ejecución

Ejecutar la suite de tests:

```bash
mvn clean test
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

Según el reporte HTML oficial de JaCoCo (fuente de verdad definida por la guía de evaluación), el paquete `com.magictickets.domain` muestra:

- **Instructions:** 100% (0 de 92 sin cubrir)
- **Branches:** 100% (0 de 6 sin cubrir)
- **Methods:** 100% (0 de 12 sin cubrir)
- **Classes:** 100% (0 de 5 sin cubrir)

**Nota sobre una discrepancia observada:** el plugin adicional `jacoco-console-reporter` (que imprime un resumen en la terminal al ejecutar `mvn clean test`) reporta 83,33% de cobertura de clases (5/6) en lugar de 100%. Esto ocurre porque ese plugin contabiliza `PurchaseNotifier` como una clase independiente sin instrucciones ejecutables (al ser una interfaz sin cuerpo de método), mientras que el reporte HTML oficial de JaCoCo no la computa de la misma forma. Se prioriza el reporte HTML por ser la fuente de verdad explícita de la guía de evaluación.
