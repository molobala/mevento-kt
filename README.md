# MEvento Kotlin

MEvento is a tiny single-file scripting VM for host applications. The host keeps
control of native behavior by exposing functions, while scripts stored in a
database or loaded at runtime can compose those functions without rebuilding the
host app.

This module is the Kotlin/JVM implementation. It is synchronous by default, with
an optional coroutine artifact for suspend host functions.

## v2 Status

v2 is a superset of v1 script syntax, with stricter diagnostics and new
runtime helpers. Existing v1 scripts should still parse and execute in v2, with
one intentional runtime difference: unknown host functions now raise
`unknown_function` instead of silently returning `null`. Wrap a risky call in
`_try_` when a script should continue after failure.

v2 additions include:

- Dot property access: `user.name` is equivalent to `user['name']`.
- `_try_` result capture and helper functions.
- Function specs with arity, argument type hints, tags, and return type hints.
- Script manifest validation for required host functions, inputs, and outputs.
- Trace mode and execution step budgets.
- Minimal collection built-ins for arrays and maps.

## Script Syntax

MEvento is intentionally small and C-like. It has no user-defined functions and
no classes. The host application owns the real logic.

Supported syntax includes:

- Assignments and expressions.
- Host function calls.
- Object and array literals.
- Bracket access and assignment: `obj['name']`, `items[0] = value`.
- Dot access for simple object keys: `obj.name`.
- `if` / `else`.
- `while`.
- Numeric `for`.
- `for ... in`.
- `break`, `continue`, `return`.
- `??`, `&&`, `||`.
- Line comments with `#` and block comments with `#* ... *#`.
- English, French, and Bambara keyword dictionaries.

Example:

```js
user = loadUser()
name = user.profile.name ?? 'anonymous'

items = []
_push_(items, name)

if (_len_(items) > 0) {
    log(items[0])
}
```

## Built-In Functions

Result helpers:

```text
_try_(expression)        # returns {ok, value, error}; evaluates lazily
_ok_(result)             # true when a _try_ result succeeded
_err_(result)            # true when a _try_ result failed
_value_(result, fallback)
_error_(result)
_code_(result)
_message_(result)
_unwrap_(result)         # returns value or rethrows the captured runtime error
```

Collection helpers:

```text
_len_(array|object|string)
_push_(array, value)        # mutates array, returns array
_pop_(array)                # mutates array, returns removed value or null
_insert_(array, index, value)
_remove_at_(array, index)   # mutates array, returns removed value or null
_has_(object, key)
_keys_(object)
_values_(object)
```

Invalid built-in argument types raise `invalid_argument_type` and can be
captured with `_try_`.

## Usage

Register global functions:

```kotlin
import com.ml.labs.MEvento
import kotlin.math.cos

MEvento.register("log") { args, _ ->
    args.forEach { println(it) }
    null
}

MEvento.register("cos2") { args, _ ->
    cos((args[0] as Number).toDouble())
}

val result = MEvento.run("cos2(0)")
```

Register functions on one VM instance:

```kotlin
import com.ml.labs.MEvento
import com.ml.labs.MEventoArgSpec
import com.ml.labs.MEventoFunctionSpec

val vm = MEvento.newInstance()

vm.registerFunction(
    "add",
    MEventoFunctionSpec(
        "add",
        minArgs = 2,
        maxArgs = 2,
        args = listOf(
            MEventoArgSpec("left", "number"),
            MEventoArgSpec("right", "number"),
        ),
        returnType = "number",
    )
) { args, _ ->
    (args[0] as Number).toLong() + (args[1] as Number).toLong()
}

val value = vm.execute("result = add(12, 23); result")
```

Inject host input for one run:

```kotlin
val total = MEvento.run(
    "base + bonus",
    input = mapOf("base" to 12, "bonus" to 23),
)
```

## Validation And Manifests

Function specs drive preflight validation and runtime argument checks:

```kotlin
val validation = vm.validate("add(12)")
if (!validation.ok) {
    println(validation.errors)
}
```

Manifest validation checks required functions, inputs, and outputs:

```kotlin
import com.ml.labs.MEventoScriptManifest
import com.ml.labs.MEventoValueSpec

val manifest = MEventoScriptManifest(
    functions = mapOf("add" to MEventoFunctionSpec("add", minArgs = 2, maxArgs = 2)),
    inputs = listOf(MEventoValueSpec("base", "number")),
    outputs = listOf(MEventoValueSpec("result", "number")),
)

val checked = MEvento.validateManifest(
    "result = add(base, 2)",
    manifest,
)
```

## Trace And Budgets

Use `MEventoOptions` to cap execution steps or collect a lightweight trace:

```kotlin
import com.ml.labs.MEventoOptions

val vm = MEvento.newInstance(MEventoOptions(maxSteps = 1000, trace = true))
vm.execute("log('ok')")

val trace = vm.trace()
```

## Coroutine Async Artifact

The core artifact stays synchronous and does not depend on coroutines. Coroutine
support lives in the separate `mevento-coroutines` artifact. It provides
`MEventoAsync`, `MEvento.newAsyncInstance()`, and suspend host-function
registration helpers.

Build the core artifact locally first, then build the coroutine artifact:

```bash
mvn install
mvn -f async/pom.xml test
```

With JitPack, add the repository as usual:

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

The existing synchronous artifact remains:

```kotlin
implementation("com.github.molobala:mevento-kt:<tag>")
```

The coroutine artifact is published as a separate module for new tags:

```kotlin
implementation("com.github.molobala.mevento-kt:mevento-coroutines:<tag>")
```

Example:

```kotlin
import com.ml.labs.MEventoAsync
import kotlinx.coroutines.runBlocking

runBlocking {
    val mevento = MEventoAsync.newInstance()
    val result = mevento.execute("a = 12; b = 23; a + b").await()
}
```

## Development

```bash
mvn test
mvn install
mvn -f async/pom.xml test
```
