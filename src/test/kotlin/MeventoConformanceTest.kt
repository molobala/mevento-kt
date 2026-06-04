import com.ml.labs.MEvento
import com.ml.labs.MEventoFunctionSpec
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

    @Test
    fun `v2 validation conformance corpus`() {
        readValidationCases().forEach { case ->
            val result = MEvento.validate(case.source, case.functionSpecs)

            assertEquals(case.ok, result.ok, case.id)
            if (!case.ok) {
                assertTrue(
                    result.errors.any { it.code == case.code },
                    "${case.id}: expected validation error `${case.code}`, got ${result.errors}",
                )
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

    private fun readValidationCases(): List<ValidationCase> {
        val file = File("conformance/v2_validation.tsv")
        assertTrue(file.exists(), "Missing validation corpus: ${file.absolutePath}")

        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split("\t", limit = 5)
                assertEquals(5, parts.size, "Invalid validation row: $line")
                ValidationCase(
                    id = parts[0],
                    functionSpecs = parseFunctionSpecs(parts[1]),
                    ok = parts[2].toBooleanStrict(),
                    code = parts[3],
                    source = decodeEscapes(parts[4]),
                )
            }
    }

    private fun parseFunctionSpecs(value: String): Map<String, MEventoFunctionSpec> {
        if (value == "-") return emptyMap()
        return value.split(",")
            .filter { it.isNotBlank() }
            .associate { entry ->
                val parts = entry.split(":", limit = 2)
                val name = parts[0]
                if (parts.size == 1) {
                    name to MEventoFunctionSpec(name)
                } else {
                    val (minArgs, maxArgs) = parseArity(parts[1])
                    name to MEventoFunctionSpec(name, minArgs = minArgs, maxArgs = maxArgs)
                }
            }
    }

    private fun parseArity(value: String): Pair<Int?, Int?> {
        val parts = value.split("..", limit = 2)
        if (parts.size == 1) {
            val exact = parts[0].toInt()
            return exact to exact
        }
        val minArgs = parts[0].takeIf { it != "*" }?.toInt()
        val maxArgs = parts[1].takeIf { it != "*" }?.toInt()
        return minArgs to maxArgs
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

    data class ValidationCase(
        val id: String,
        val functionSpecs: Map<String, MEventoFunctionSpec>,
        val ok: Boolean,
        val code: String,
        val source: String,
    )
}
