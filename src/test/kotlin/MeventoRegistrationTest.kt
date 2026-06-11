import com.ml.labs.MEvento
import com.ml.labs.MEventoArgSpec
import com.ml.labs.MEventoFunctionExample
import com.ml.labs.MEventoFunctionSpec
import com.ml.labs.MEventoOptions
import com.ml.labs.MEventoRuntimeError
import com.ml.labs.MEventoScriptManifest
import com.ml.labs.MEventoValueSpec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeventoRegistrationTest {
    var vm: MEvento = MEvento()

    @Test
    fun `Should be able to register a function globally`() {
        MEvento.register("log") { _, _ ->
            true
        }
        MEvento.run("log()").also {
            assertEquals(true, it)
        }
    }

    @Test
    fun `Should be able to register a function via instance`() {
        vm.registerFunction("log2") { _, _ ->
            "log2"
        }
        assertThrows<MEventoRuntimeError> {
            MEvento.run("log2()")
        }
        vm.execute("log2()").also {
            assertEquals("log2", it)
        }
    }

    @Test
    fun `Should be able to handle function arguments`() {
        vm.registerFunction("argLog") { args, _ ->
            args[0]
        }
        vm.execute("argLog('Hi')").also {
            assertEquals("Hi", it)
        }
    }

    @Test
    fun `Should validate registered function arity metadata`() {
        vm.registerFunction("one", MEventoFunctionSpec("one", minArgs = 1, maxArgs = 1)) { args, _ ->
            args.firstOrNull()
        }

        val validation = vm.validate("one()")
        assertFalse(validation.ok)
        val validationError = validation.errors.first { it.code == "invalid_function_arity" }
        assertEquals("one", validationError.name)
        assertEquals(0, validationError.argCount)
        assertEquals(1, validationError.minArgs)
        assertEquals(1, validationError.maxArgs)

        val error = assertThrows<MEventoRuntimeError> {
            vm.execute("one()")
        }
        assertEquals("invalid_function_arity", error.code)
        assertEquals("one", error.name)
        assertEquals(0, error.argCount)
        assertEquals(1, error.minArgs)
        assertEquals(1, error.maxArgs)
        assertTrue(error.message?.contains("expects 1 argument") == true)

        val tryError = vm.execute("result = _try_(one()); result['error']") as Map<*, *>
        assertEquals("invalid_function_arity", tryError["code"])
        assertEquals("one", tryError["name"])
        assertEquals(0, tryError["argCount"])
        assertEquals(1, tryError["minArgs"])
        assertEquals(1, tryError["maxArgs"])
    }

    @Test
    fun `Should expose instance function capabilities`() {
        vm.registerFunction(
            "tagged",
            MEventoFunctionSpec(
                "tagged",
                minArgs = 1,
                maxArgs = 2,
                tags = setOf("pure"),
                args = listOf(
                    MEventoArgSpec(
                        "message",
                        "string",
                        description = "Message to return.",
                        metadata = mapOf("ui" to mapOf("control" to "text")),
                    )
                ),
                returnType = "string",
                description = "Returns a tagged message.",
                returnDescription = "The original message.",
                examples = listOf(
                    MEventoFunctionExample(
                        title = "Return message",
                        script = "tagged('hello')",
                        result = mapOf("value" to "hello"),
                        description = "Returns the provided message.",
                    )
                ),
                metadata = mapOf(
                    "category" to "demo",
                    "permissions" to listOf("none"),
                ),
            )
        ) { args, _ ->
            args.firstOrNull()
        }

        val spec = vm.capabilities()["tagged"]
        assertEquals("tagged", spec?.name)
        assertEquals(1, spec?.minArgs)
        assertEquals(2, spec?.maxArgs)
        assertTrue(spec?.tags?.contains("pure") == true)
        assertEquals("message", spec?.args?.first()?.name)
        assertEquals("string", spec?.args?.first()?.type)
        assertEquals("Message to return.", spec?.args?.first()?.description)
        assertEquals(mapOf("control" to "text"), spec?.args?.first()?.metadata?.get("ui"))
        assertEquals("string", spec?.returnType)
        assertEquals("Returns a tagged message.", spec?.description)
        assertEquals("The original message.", spec?.returnDescription)
        assertEquals("Return message", spec?.examples?.first()?.title)
        assertEquals("tagged('hello')", spec?.examples?.first()?.script)
        assertEquals(mapOf("value" to "hello"), spec?.examples?.first()?.result)
        assertEquals("Returns the provided message.", spec?.examples?.first()?.description)
        assertEquals("demo", spec?.metadata?.get("category"))
        assertEquals(listOf("none"), spec?.metadata?.get("permissions"))

        @Suppress("UNCHECKED_CAST")
        (spec?.metadata?.get("permissions") as MutableList<String>).add("mutated")
        @Suppress("UNCHECKED_CAST")
        ((spec.args.first().metadata["ui"] as MutableMap<String, String>)["control"]) = "mutated"
        @Suppress("UNCHECKED_CAST")
        ((spec.examples.first().result as MutableMap<String, String>)["value"]) = "mutated"
        val cloned = vm.capabilities()["tagged"]
        assertEquals(listOf("none"), cloned?.metadata?.get("permissions"))
        assertEquals(mapOf("control" to "text"), cloned?.args?.first()?.metadata?.get("ui"))
        assertEquals(mapOf("value" to "hello"), cloned?.examples?.first()?.result)
    }

    @Test
    fun `Should validate registered function argument type metadata`() {
        vm.registerFunction(
            "text",
            MEventoFunctionSpec(
                "text",
                minArgs = 1,
                maxArgs = 1,
                args = listOf(MEventoArgSpec("value", "string")),
            )
        ) { args, _ ->
            args.firstOrNull()
        }

        val validation = vm.validate("text(12)")
        assertFalse(validation.ok)
        val validationError = validation.errors.first { it.code == "invalid_argument_type" }
        assertEquals("text", validationError.name)
        assertEquals(0, validationError.argIndex)
        assertEquals("string", validationError.expectedType)
        assertEquals("number", validationError.actualType)

        val runtimeError = assertThrows<MEventoRuntimeError> {
            vm.execute("value = 12; text(value)")
        }
        assertEquals("invalid_argument_type", runtimeError.code)
        assertEquals("text", runtimeError.name)
        assertEquals(0, runtimeError.argIndex)
        assertEquals("string", runtimeError.expectedType)
        assertEquals("number", runtimeError.actualType)

        val tryError = vm.execute("value = 12; result = _try_(text(value)); result['error']") as Map<*, *>
        assertEquals("invalid_argument_type", tryError["code"])
        assertEquals("text", tryError["name"])
        assertEquals(0, tryError["argIndex"])
        assertEquals("string", tryError["expectedType"])
        assertEquals("number", tryError["actualType"])
    }

    @Test
    fun `Should support v1 compatibility mode for unknown functions`() {
        val strictError = assertThrows<MEventoRuntimeError> {
            MEvento().execute("missing()")
        }
        assertEquals("unknown_function", strictError.code)

        val compatible = MEvento(options = MEventoOptions(compatV1 = true))
        assertEquals(null, compatible.execute("missing()"))
        assertEquals(12L, compatible.execute("missing(); 12"))
        assertTrue(compatible.validate("missing()").ok)

        val tryResult = compatible.execute("result = _try_(missing()); result") as Map<*, *>
        assertEquals(true, tryResult["ok"])
        assertEquals(null, tryResult["value"])
    }

    @Test
    fun `Should stop execution when step budget is exceeded`() {
        val limited = MEvento(options = MEventoOptions(maxSteps = 20))
        val runtimeError = assertThrows<MEventoRuntimeError> {
            limited.execute("a = 1; while(a > 0) { a = a + 1; }")
        }

        assertEquals("execution_budget_exceeded", runtimeError.code)
        assertEquals(20L, runtimeError.maxSteps)
        assertTrue((runtimeError.stepCount ?: 0L) > 20L)

        val tryResult = MEvento(options = MEventoOptions(maxSteps = 2)).execute("_try_(1)") as Map<*, *>
        val tryError = tryResult["error"] as Map<*, *>
        assertEquals("execution_budget_exceeded", tryError["code"])
        assertEquals(2L, tryError["maxSteps"])
        assertTrue((tryError["stepCount"] as Long) > 2L)
    }

    @Test
    fun `Should validate scripts against a manifest`() {
        val manifest = MEventoScriptManifest(
            functions = mapOf(
                "add" to MEventoFunctionSpec(
                    "add",
                    minArgs = 2,
                    maxArgs = 2,
                    returnType = "number",
                )
            ),
            inputs = listOf(MEventoValueSpec("base", "number")),
            outputs = listOf(MEventoValueSpec("result", "number")),
        )

        val valid = vm.validateManifest("result = add(base, 2)", manifest)
        assertTrue(valid.ok)

        val invalid = vm.validateManifest("result = 'bad'; other(missing)", manifest)
        assertFalse(invalid.ok)
        assertTrue(invalid.errors.any { it.code == "unknown_function" && it.name == "other" })
        assertTrue(invalid.errors.any { it.code == "unknown_input" && it.name == "missing" })
        assertTrue(invalid.errors.any { it.code == "invalid_output_type" && it.name == "result" && it.expectedType == "number" && it.actualType == "string" })

        val missingOutput = vm.validateManifest("value = add(base, 2)", manifest)
        assertTrue(missingOutput.errors.any { it.code == "missing_output" && it.name == "result" })

        val dotManifest = MEventoScriptManifest(
            inputs = listOf(MEventoValueSpec("user", "object")),
            outputs = listOf(MEventoValueSpec("result")),
        )
        val validDotAccess = vm.validateManifest("result = user.name", dotManifest)
        assertTrue(validDotAccess.ok)

        val missingOwner = vm.validateManifest(
            "result = user.name",
            dotManifest.copy(inputs = listOf(MEventoValueSpec("name", "string"))),
        )
        assertTrue(missingOwner.errors.any { it.code == "unknown_input" && it.name == "user" })
        assertFalse(missingOwner.errors.any { it.code == "unknown_input" && it.name == "name" })
    }

    @Test
    fun `Should record trace events when trace mode is enabled`() {
        val traced = MEvento(options = MEventoOptions(trace = true))
        traced.registerFunction(
            "text",
            MEventoFunctionSpec(
                "text",
                minArgs = 1,
                maxArgs = 1,
                args = listOf(MEventoArgSpec("value", "string")),
                returnType = "string",
            )
        ) { args, _ ->
            args.firstOrNull()
        }

        assertEquals("ok", traced.execute("text('ok')"))
        val trace = traced.trace()
        assertTrue(trace.any { it.kind == "visit" && it.node == "CallExpressionAST" })
        val call = trace.first { it.kind == "call" && it.name == "text" }
        assertEquals(1, call.detail["argCount"])
        assertEquals("string", call.detail["returnType"])
        val result = trace.first { it.kind == "call_result" && it.name == "text" }
        assertEquals("string", result.detail["returnType"])
        assertEquals("string", result.detail["actualType"])
    }

    @Test
    fun `Should expose helpers for try results`() {
        assertEquals(true, vm.execute("result = _try_(1 + 2); _ok_(result)"))
        assertEquals(false, vm.execute("result = _try_(1 + 2); _err_(result)"))
        assertEquals(3L, vm.execute("result = _try_(1 + 2); _value_(result, 99)"))
        assertEquals(null, vm.execute("result = _try_(1 + 2); _error_(result)"))
        assertEquals("fallback", vm.execute("result = _try_(missing()); _value_(result, 'fallback')"))
        assertEquals("unknown_function", vm.execute("result = _try_(missing()); _code_(result)"))
        assertTrue((vm.execute("result = _try_(missing()); _message_(result)") as String).contains("Unknown function"))
        assertEquals("unknown_function", vm.execute("result = _try_(missing()); _error_(result)['code']"))
        assertEquals(false, vm.execute("_ok_(12)"))
        assertEquals(true, vm.execute("_err_(12)"))
        assertEquals(null, vm.execute("_code_(12)"))
        assertEquals(3L, vm.execute("_unwrap_(_try_(1 + 2))"))

        val unwrapError = assertThrows<MEventoRuntimeError> {
            vm.execute("_unwrap_(_try_(missing()))")
        }
        assertEquals("unknown_function", unwrapError.code)

        val okSpec = vm.capabilities()["_ok_"]
        assertEquals(1, okSpec?.minArgs)
        assertEquals(1, okSpec?.maxArgs)
        assertEquals("boolean", okSpec?.returnType)

        val pushSpec = vm.capabilities()["_push_"]
        assertEquals(2, pushSpec?.minArgs)
        assertEquals("array", pushSpec?.args?.first()?.type)
        assertEquals("array", pushSpec?.returnType)

        val keysSpec = vm.capabilities()["_keys_"]
        assertEquals("object", keysSpec?.args?.first()?.type)
        assertEquals("array", keysSpec?.returnType)
    }

    @Test
    fun `Should expose global function capabilities`() {
        MEvento.register("globalCap", MEventoFunctionSpec("globalCap", maxArgs = 0, tags = setOf("io"))) { _, _ ->
            true
        }
        try {
            val spec = MEvento.capabilities()["globalCap"]
            assertEquals("globalCap", spec?.name)
            assertEquals(0, spec?.maxArgs)
            assertTrue(spec?.tags?.contains("io") == true)
        } finally {
            MEvento.unregister("globalCap")
        }
    }

    @Test
    fun `Should inject input for static run`() {
        MEvento.run("a + b", input = mapOf("a" to 12L, "b" to 23L)).also {
            assertEquals(35L, it)
        }
    }
}
