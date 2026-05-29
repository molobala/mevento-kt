# Mevento

Mevento is a tiny VM one single file that handles `MEvento code` executions inside JS engine. `MEvento` is simple programming language that allows developers exposing an app host function, that way they have the ability to dynamically execute simple script that call host function.

The VM uses a AST Walker to execute MEvento script, so of course the performace is not its concern a lot.

## MEvento code syntax
Syntaxically MEvento is a c-like language, but very limited: no function declaration, no class, just assignation, function call and conditional check.

```js
a = 12
a = 23
b = functon1()
c = function2()
d = a + b
if(a == b) {
    log('a = b')
} else {
    log("a != b")
}
```

## How to use
The host application can expose functions through Mevento VM that way:
```kotlin
import com.ml.labs.MEvento
import kotlin.math.cos

MEvento.register("log") { args, _ ->  args.forEach { println(it) }} // exposes console.log through MEvento as log function
MEvento.register("cos2") {args, _ ->  cos((args[0] as Double))}
```

Let's assume you want to execute a `MEvento code`:

```kotlin
import com.ml.labs.MEvento

val mevento = MEvento.newInstance()

mevento.execute("""log("molo")""")

```

## Coroutine async artifact

The core artifact stays synchronous and does not depend on coroutines. Coroutine support lives in the separate `mevento-coroutines` artifact. It provides `MEventoAsync`, `MEvento.newAsyncInstance()`, and suspend host-function registration helpers.

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

After tagging, look up `molobala/mevento-kt` on JitPack to confirm that `mevento-coroutines` appears in the module list for that tag.

Example:

```kotlin
import com.ml.labs.MEventoAsync
import kotlinx.coroutines.runBlocking

runBlocking {
    val mevento = MEventoAsync.newInstance()
    val result = mevento.execute("a = 12; b = 23; a + b").await()
}
```
