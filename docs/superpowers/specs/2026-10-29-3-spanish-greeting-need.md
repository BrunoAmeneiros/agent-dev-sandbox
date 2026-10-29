# Need: Spanish greeting

Feature 3 · feature/3-spanish-greeting · 2026-10-29

**Outcome:** build-missing

## Problem

Users of our new client in Spain see "Hello, Ana!" on the app's home screen. Several have complained that the app is not in their language. They should see "¡Hola, Ana!". If nothing is done, the complaints from a new client continue.

## What exists

- `sandbox.Greeter.greet(name)` always returns `"Hello, <name>!"`, English only.
- No ADRs or earlier decisions about language.
- The JDK already offers `Locale` and `ResourceBundle`. External i18n libraries (ICU4J, etc.) exist but are excluded by the constraints.

## Success

- A user of the Spanish client sees "¡Hola, Ana!".
- Everyone else keeps seeing "Hello, Ana!" (the approved acceptance test still passes).

## Constraints

- No new dependencies: JDK and existing code only.
- Only Spanish and English; no other languages planned.
- The language is decided per client (all users of that client are in Spain); no per-user preference is needed.
- No hard deadline.

## Outcomes considered

1. `dont-build`: rejected. Real complaints, and nothing outside this repo changes the text `Greeter` produces.
2. `adopt`: an i18n library or message framework. Ruled out by "no new dependencies", and oversized for two languages.
3. `build-missing`: keep `Greeter` and add only language selection (es/en) and the Spanish template, on top of what the JDK offers. English stays unchanged. **Recommended.**
4. `build-all`: a full i18n system (preferences, many languages, catalogs). There is no demand for it.

## Decision

Your human partner chose `build-missing` ("Pasa al diseño"): it is the smallest change that solves the complaint within the no-dependencies constraint.

## For the design

- How the per-client language reaches `Greeter` (parameter, constructor, `Locale`).
- Default when the language is unknown or missing (presumably English).
- Plain map/switch vs. `ResourceBundle` for two languages.
