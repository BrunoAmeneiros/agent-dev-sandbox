# Need: saludo en español con `--lang es`

Feature 9 · feature/9-spanish-greeting · 2026-11-03

**Outcome:** build-missing

## Problem

Hay usuarios hispanohablantes que usan la CLI en scripts de bienvenida y han pedido el saludo en español. Hoy la CLI solo saluda en inglés (`Hello, Ana!`). Corre prisa porque el mes que viene entra un cliente nuevo en España. Si no se hace nada, ese cliente recibe los saludos en inglés o tiene que traducir la salida en sus propios scripts.

## What exists

- `GreetingCli` acepta `<name> [--shout]` e imprime lo que devuelve `Greeter.greet`.
- `Greeter.greet(name)` devuelve siempre `"Hello, <name>!"`.
- No hay ADRs sobre idiomas ni internacionalización.
- El JDK trae `Locale` y `ResourceBundle`, sin dependencias nuevas. Ninguna parte del proyecto soporta hoy otro idioma.

## Success

- `greet Ana --lang es` imprime `¡Hola, Ana!`.
- Sin `--lang`, la salida sigue siendo `Hello, Ana!`.
- Con un idioma que no existe (por ejemplo `--lang xx`), la CLI avisa del error, no saluda y termina con un código de salida distinto de cero.
- Solo español por ahora.

## Constraints

- Tiene que estar listo antes de que entre el cliente, en unas tres semanas.
- Los scripts actuales no pueden cambiar: `--shout` y la salida en inglés tienen que seguir idénticas.
- Sin dependencias nuevas.
- Lo mantiene el mismo equipo.

## Outcomes considered

1. `dont-build`: descartada. Lo han pedido usuarios reales y hay un cliente con fecha. Que cada script traduzca la salida por su cuenta (con `sed`, por ejemplo) es frágil y saca la lógica del repo.
2. `adopt`: descartada. No hay herramienta ni librería externa que adoptar, y la regla de no añadir dependencias lo impide. Lo que da el JDK es una forma de construirlo, no un sustituto.
3. `build-missing`: **recomendada**. `Greeter` y `GreetingCli` ya existen y funcionan. Solo falta el flag `--lang es`, el saludo en español y el error para idiomas desconocidos, sin tocar el comportamiento actual. Es pequeño y cabe en el plazo.
4. `build-all`: descartada. Rehacer la CLI o montar un sistema de i18n completo para un solo idioma pone en riesgo la compatibilidad de los scripts sin ganar nada.

## Decision

Bruno elige `build-missing`, como se recomendó: añadir solo lo que falta sobre la CLI actual, sin dependencias nuevas y sin cambiar la salida de hoy.

## For the design

- Códigos de salida: hoy `main` siempre termina con 0. Hay que decidir cómo devuelve `run` un error y con qué código termina `main`.
- Dónde va el mensaje de error (stdout o stderr) y qué dice exactamente.
- Cómo conviven `--lang` y `--shout`: orden de los flags, si se pueden combinar y cómo sale `--shout` en español (`¡HOLA, ANA!`).
- Dónde vive el texto en español: en `Greeter` (por ejemplo `greet(name, locale)`) o en otra pieza, y si usar `ResourceBundle` o algo más simple para un solo idioma.
- Qué pasa con `--lang en` explícito y con `--lang` sin valor.
- El texto de uso (`Usage: greet <name> [--shout]`) tendrá que mencionar `--lang`.
