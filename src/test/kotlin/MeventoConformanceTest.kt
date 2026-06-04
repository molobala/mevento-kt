import com.ml.labs.MEvento
import com.ml.labs.MEventoRuntimeError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class MeventoConformanceTest {
    @Test
    fun `v2 core conformance corpus`() {
        readCases().forEach { case ->
            val vm = MEvento()
            vm.registerFunction("echo") { args, _ -> args.firstOrNull() }

            if (case.type == "error") {
                val error = assertThrows<MEventoRuntimeError>(case.id) {
                    vm.execute(case.source)
                }
                assertTrue(
                    error.message?.contains(case.expected) == true,
                    "${case.id}: expected error containing `${case.expected}`, got `${error.message}`",
                )
            } else {
                val actual = vm.execute(case.source)
                assertValue(case, actual)
            }
        }
    }

    private fun readCases(): List<ConformanceCase> {
        val file = File("conformance/v2_core.tsv")
        assertTrue(file.exists(), "Missing conformance corpus: ${file.absolutePath}")

        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split("\t", limit = 4)
                assertEquals(4, parts.size, "Invalid conformance row: $line")
                ConformanceCase(
                    id = parts[0],
                    type = parts[1],
                    expected = decodeEscapes(parts[2]),
                    source = decodeEscapes(parts[3]),
                )
            }
    }

    private fun assertValue(case: ConformanceCase, actual: Any?) {
        when (case.type) {
            "null" -> assertNull(actual, case.id)
            "number" -> {
                assertTrue(actual is Number, "${case.id}: expected number, got $actual")
                assertEquals(case.expected.toDouble(), (actual as Number).toDouble(), 0.0, case.id)
            }
            "string" -> assertEquals(case.expected, actual, case.id)
            "boolean" -> assertEquals(case.expected.toBooleanStrict(), actual, case.id)
            else -> throw AssertionError("${case.id}: unsupported expected type ${case.type}")
        }
    }

    private fun decodeEscapes(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    '\\' -> out.append('\\')
                    else -> out.append(value[i + 1])
                }
                i += 2
            } else {
                out.append(value[i])
                i++
            }
        }
        return out.toString()
    }

    data class ConformanceCase(
        val id: String,
        val type: String,
        val expected: String,
        val source: String,
    )
}
