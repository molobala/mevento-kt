import com.ml.labs.MEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        MEvento.run("log2()").also {
            assertNull(it)
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
}