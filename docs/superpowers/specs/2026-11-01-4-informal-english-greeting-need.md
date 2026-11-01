# Need: Saludo informal en inglés

Feature 4 · feature/4-informal-english-greeting · 2026-11-01

**Outcome:** build-missing

## Problem

Un cliente nuevo e importante, una cadena de gimnasios, considera que "Hello, Ana!" suena demasiado formal para su app y quiere "Hi, Ana!". Entra el mes que viene. De momento es el único cliente que lo pide.

Si no se hace nada, se quedan con "Hello" y se quejarán, pero no se perdería el cliente.

## What exists

- `Greeter.greet(name)` devuelve siempre `"Hello, <name>!"`. No hay concepto de cliente ni de preferencias de saludo.
- No hay capa de plantillas ni de traducciones, ni en este repo ni en otro servicio.
- La app del cliente podría reemplazar "Hello" por "Hi" en su lado, pero eso no se quiere: el saludo es nuestro y los clientes no deben parchear nuestro texto.
- No hay ADRs previos sobre el tema.

## Success

- El cliente de gimnasios recibe "Hi, Ana!".
- Todos los demás clientes siguen recibiendo exactamente "Hello, Ana!" sin cambiar nada de su lado.

No hace falta que se pueda activar para otros clientes sin tocar código, ni que existan otros niveles de informalidad. Si otro cliente lo pide, será otra Feature.

## Constraints

- Fecha: el cliente entra el mes que viene.
- `greet(name)` tiene que seguir funcionando exactamente igual para quien ya lo usa.
- Sin dependencias nuevas.
- Lo mantiene el mismo equipo de siempre.

## Outcomes considered

1. `dont-build`: es viable, porque no se perdería el cliente. Pero es un cliente importante con una petición clara y barata, y la alternativa (que parcheen el texto en su app) es justo lo que no se quiere. No compensa.
2. `adopt`: no hay nada que adoptar. No existe una capa de plantillas o traducciones, y no se quieren dependencias nuevas. Usar algo del JDK como `ResourceBundle` para una sola variante sería montar más de lo necesario.
3. `build-missing`: añadir solo la variante informal ("Hi, …!") encima de `Greeter`, sin cambiar `greet(name)`. Cubre el criterio de éxito con el mínimo cambio. **Recomendado.**
4. `build-all`: un sistema de saludos configurable por cliente, con niveles y activación sin código. Descartado expresamente.

## Decision

`build-missing`. Se añade solo la variante informal en inglés y `greet(name)` no cambia. Es el mínimo que satisface al cliente nuevo antes de su entrada y respeta las restricciones (compatibilidad y sin dependencias).

## For the design

- Cómo se pide la variante informal: un método nuevo, un parámetro o una opción en el constructor de `Greeter`.
- Dónde se decide que el cliente de gimnasios recibe la versión informal: en este repo o en quien llama a `Greeter`.
