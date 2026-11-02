# Design: Saludo informal en inglés

Feature 4 · feature/4-informal-english-greeting · 2026-11-02

Necesidad aprobada: [2026-11-01-4-informal-english-greeting-need.md](2026-11-01-4-informal-english-greeting-need.md). El porqué, el éxito, las restricciones y las alternativas están allí y no se repiten aquí.

## Intención

`Greeter` es una librería. Quien la llama ya sabe a qué cliente atiende y decide si quiere el saludo informal. `Greeter` solo tiene que ofrecer una forma de pedir la variante informal en inglés y devolver `"Hi, <name>!"`.

## Objetivos

- Quien pide la variante informal recibe `"Hi, <name>!"`. Por ejemplo, `"Hi, Ana!"` para `"Ana"`.
- Quien no la pide recibe exactamente lo mismo que hoy: `"Hello, <name>!"`.

## No objetivos

- Saber qué clientes quieren el saludo informal. Lo decide quien llama, no la librería: no hay lista de clientes ni configuración dentro de `Greeter`.
- Activar el saludo informal sin tocar código, u ofrecer otros niveles de informalidad u otros idiomas. Si se piden, serán otra Feature.

## Anti-objetivos

- Cualquier cambio en la firma o en el comportamiento de `greet(String name)`.
- Dependencias nuevas.

## Lo decidido

Lo ha decidido Bruno:

- La decisión de usar la variante informal se toma fuera de `Greeter`, en quien la llama.
- El formato es el del saludo formal cambiando solo la palabra: `"Hi, " + name + "!"`. El nombre se usa tal cual llega, igual que en `greet`.

Mi elección (Bruno lo dejó a mi criterio):

- **Un método nuevo, `public String greetInformally(String name)`, en `Greeter`.** Es lo más simple que cubre la necesidad: no toca `greet`, no añade tipos nuevos y quien llama lo usa sin aprender nada más. He descartado un parámetro de estilo (`greet(name, Style)`) y una opción en el constructor porque solo se justifican si hay más variantes, y la necesidad las descarta expresamente.

## Pruebas

- Prueba unitaria en `GreeterTest`: `greetInformally("Ana")` devuelve `"Hi, Ana!"`.
- Las pruebas existentes de `greet` (unitaria y de aceptación) siguen pasando sin cambios. Demuestran que el saludo formal no cambia.
- Una prueba de aceptación nueva para el saludo informal, en `tests/acceptance/`, aparte de `GreeterAcceptanceTest.java`, que está aprobado y protegido y no se toca. Como manda el proceso, se escribe en la fase de Tasks, junto con el plan, y Bruno la aprueba en la misma decisión que el plan, antes de la ejecución. Al aprobarla pasa a ser ruta protegida: quien implementa no la escribe ni la cambia. El plan no necesita ningún paso propio de aprobación ni tocar `.process/config.json`.

## Lo que queda al constructor

- El nombre del archivo y de la clase de la prueba de aceptación nueva, y el nombre de ejemplo que use.
- El Javadoc del método nuevo. `checkstyle.xml` no lo exige.
