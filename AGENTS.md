# Mevento Kotlin Agent Notes

## Scope

This folder contains the Kotlin/JVM implementation of Mevento. These instructions apply to `kt/` and its subdirectories.

## Project Shape

- Core implementation: `src/main/kotlin/Mevento.kt`
- Tests: `src/test/kotlin/MeventoKtTest.kt` and `src/test/kotlin/MeventoRegistrationTest.kt`
- Build metadata: `pom.xml`
- Coroutine async artifact: `async/`
- Maven output: `target/`

The implementation keeps lexer, parser, AST model, and runtime walker in one Kotlin file. Follow that structure for focused fixes unless a larger refactor is requested.

## Development Commands

- Run tests: `mvn test`
- Build package: `mvn package`
- Run coroutine artifact tests after installing the core artifact locally: `mvn install` then `mvn -f async/pom.xml test`

The Maven project uses Kotlin 1.8.0, JUnit 5, and JVM target 1.8.

## Implementation Guidance

- Preserve behavior parity with `../ts/src/index.ts` and `../../flutter_mevento/lib/mevento.dart` for script syntax and runtime semantics.
- Keep package declarations as `com.ml.labs`.
- Treat `MEvento.compile`, `MEvento.run`, `MEvento.newInstance`, `execute`, `register`, `unregister`, `registerFunction`, `unregisterFunction`, `clone`, and `memory` as public API.
- Keep coroutine support in `async/` so the core artifact stays free of coroutine dependencies.
- Host functions use `(List<Any?>, MEvento?) -> Any?`; preserve argument evaluation and VM passing.
- Be careful around numeric behavior. Tests currently expect many integer arithmetic results as `Long`.
- Keep keyword dictionaries for English, French, and Bambara aligned when adding or renaming keywords.
- Parser/runtime errors often surface as thrown exceptions or `null` results depending on visitor boundaries. Add tests before changing this behavior.

## Test Focus

For engine changes, update or add tests for:

- arithmetic and string repetition
- object/array literals and index assignment
- scoping through blocks and loops
- `if`, `while`, numeric `for`, `for ... in`, `break`, `continue`, and `return`
- retained loop results
- multilingual programs using `<fr>` and `<bm>`
- global and instance function registration

## Repository Hygiene

- Do not edit `target/` outputs as source.
- Do not modify `.idea/` files unless the task explicitly concerns project metadata.
- This folder is its own Git repository. Preserve unrelated local changes; check `git status --short` before editing tracked files.
