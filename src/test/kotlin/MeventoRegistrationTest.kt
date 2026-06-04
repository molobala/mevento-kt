import com.ml.labs.MEvento
import com.ml.labs.MEventoArgSpec
import com.ml.labs.MEventoFunctionSpec
import com.ml.labs.MEventoOptions
import com.ml.labs.MEventoRuntimeError
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
                args = listOf(MEventoArgSpec("message", "string")),
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
