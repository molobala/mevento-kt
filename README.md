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
