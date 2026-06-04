import com.ml.labs.MEvento
import com.ml.labs.MEventoAsync
import com.ml.labs.MEventoRuntimeError
import com.ml.labs.newAsyncInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class MeventoAsyncTest {
    @Test
    fun `Should execute arithmetic asynchronously`() {
        runBlocking {
            val vm = MEventoAsync.newInstance()
            vm.execute("a = 12;b = 23;a + b").await().also {
                assertEquals(35L, it)
            }
        }
    }

    @Test
    fun `Should repeat strings asynchronously`() {
        runBlocking {
            val vm = MEventoAsync.newInstance()
            vm.execute("'molo' * 3").await().also {
                assertEquals("molomolomolo", it)
            }
        }
    }

    @Test
    fun `Should inject input for async run`() {
        runBlocking {
            MEventoAsync.run("a + b", input = mapOf("a" to 12L, "b" to 23L)).await().also {
                assertEquals(35L, it)
            }
        }
    }

    @Test
    fun `Should execute suspend host functions`() {
        runBlocking {
            val vm = MEventoAsync.newInstance()
            vm.registerSuspendFunction("delayed") { args, _ ->
                delay(1)
                args[0]
            }
            vm.execute("delayed('ok')").await().also {
                assertEquals("ok", it)
            }
        }
    }

    @Test
    fun `Should surface runtime errors asynchronously`() {
        runBlocking {
            val vm = MEventoAsync.newInstance()
            try {
                vm.execute("a = 1; b = 0; a / b").await()
                fail("Expected MEventoRuntimeError")
            } catch (error: MEventoRuntimeError) {
                assertEquals(true, error.message?.contains("Invalid division by 0"))
            }
        }
    }

    @Test
    fun `Should create async VM from sync VM`() {
        runBlocking {
            val vm = MEvento.newInstance()
            vm.execute("a = 12")
            val asyncVm = vm.newAsyncInstance()
            asyncVm.execute("a + b", input = mapOf("b" to 23L)).await().also {
                assertEquals(35L, it)
            }
        }
    }
}
