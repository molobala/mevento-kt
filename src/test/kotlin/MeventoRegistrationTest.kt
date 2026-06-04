import com.ml.labs.MEvento
import com.ml.labs.MEventoFunctionSpec
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
        assertTrue(validation.errors.any { it.code == "invalid_function_arity" })

        val error = assertThrows<MEventoRuntimeError> {
            vm.execute("one()")
        }
        assertTrue(error.message?.contains("expects 1 argument") == true)
    }

    @Test
    fun `Should expose instance function capabilities`() {
        vm.registerFunction("tagged", MEventoFunctionSpec("tagged", minArgs = 1, maxArgs = 2, tags = setOf("pure"))) { args, _ ->
            args.firstOrNull()
        }

        val spec = vm.capabilities()["tagged"]
        assertEquals("tagged", spec?.name)
        assertEquals(1, spec?.minArgs)
        assertEquals(2, spec?.maxArgs)
        assertTrue(spec?.tags?.contains("pure") == true)
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
