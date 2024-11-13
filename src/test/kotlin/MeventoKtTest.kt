import com.ml.labs.MEvento
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class MeventoKtTest {
    var vm: MEvento = MEvento()

    @Test
    fun `Should make arithmetic operation on number`() {
        vm.execute("a = 12;b=23;a+b").also {
            assertEquals(35L, it)
        }
        vm.execute("a = 12;b=23;a-b").also {
            assertEquals(-11L, it)
        }
        vm.execute("a = 2;b=23;a*b").also {
            assertEquals(46L, it)
        }
        vm.execute("a = 2;b=24;b/a").also {
            assertEquals(12L, it)
        }
        vm.execute("a = 0;b=24;b/a").also {
            assertNull(it)
        }
    }

    @Test
    fun `Should make arithmetic operation on string`() {
        vm.execute("a = '12';b='23';a+b").also {
            assertEquals(it, "1223")
        }
        vm.execute("'molo'*3").also {
            assertEquals(it, "molomolomolo")
        }
    }

    @Test
    fun `should parse literal string`() {
        vm.execute("'Molo\\nMala\\a\\m'").also {
            assertEquals("Molo\nMala\\am", it)
        }
        vm.execute("'Molo\\uab32'").also {
            assertEquals("Molo\uab32", it)
        }
    }

    @Test
    fun `should parse literal object`() {
        vm.execute("obj = {'key': 'molo'}\nlog(obj)\nobj").also {
            assertInstanceOf(Map::class.java, it)
            assertEquals("molo", (it as Map<*, *>)["key"])
        }
    }

    @Test
    fun `should parse change object by indexing`() {
        vm.execute("obj = {'key': 'molo'}\nlog(obj)\nobj['molo']='mol';obj['tt']=20\nobj").also {
            assertNotNull(it)
            assertInstanceOf(Map::class.java, it)
            it as Map<*, *>
            assertEquals("molo", it["key"])
            assertEquals("mol", it["molo"])
            assertEquals(20L, it["tt"])
        }
    }

    @Test
    fun `should parse literal array`() {
        vm.execute("arr = ['key', 'molo', 12, 34 + 5]\narr").also {
            assertNotNull(it)
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals("key", it[0])
            assertEquals("molo", it[1])
            assertEquals(39L, it[3])
        }
    }

    @Test
    fun `should parse change array by index`() {
        vm.execute("arr = ['key', 'molo', 12, 34 + 5]\narr[0]=23\narr[1]='changed'\narr").also {
            assertNotNull(it)
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals(23L, it[0])
            assertEquals("changed", it[1])
            assertEquals(39L, it[3])
        }
    }

    @Test
    fun `should parse get array by index`() {
        vm.execute("arr = ['key', 'molo', 12, 34 + 5]\narr[0]").also {
            assertNotNull(it)
            assertEquals("key", it)
        }
    }

    @Test
    fun `Not Equal`() {
        vm.execute("a = 12; a != 34").also {
            assertEquals(true, it)
        }
    }

    @Test
    fun `Not Op`() {
        vm.execute("a = 12; b = !(a == 12);b").also {
            assertEquals(false, it)
        }
        vm.execute("a = 12; b = !testo();b").also {
            assertEquals(true, it)
        }
    }

    @Test
    fun `False or right hand object `() {
        vm.execute("a = trufy() || 23;a").also {
            assertEquals(true, it)
        }
    }

    @Test
    fun `Single quote '`() {
        vm.execute("a = \"'\"").also {
            assertEquals(it, "'")
        }
    }

    @Test
    fun Scoping() {
        vm.execute(
            """
        a = 12; b = 0;
        if(a >= 10) {
            b = a;
            c = b + 1
        }
        c"""
        ).also {
            assertNull(it)
        }
        vm.execute(
            """
        a = 12; b = 0;
        if(a >= 10) {
            b = a;
            c = b + 1
        }
        c = b
        c"""
        ).also {
            assertEquals(12L, it)
        }
    }

    @Test
    fun `If statement`() {
        vm.execute(
            """a = 12; b = 0;
        if(a >= 10) {
            b = a;
        }
        b"""
        ).also {
            assertEquals(12L, it)
        }
    }

    @Test
    fun `Break outside loop`() {
        org.junit.jupiter.api.assertThrows<Exception> {
            vm.execute(
                """a = 12;
        if(a == 12) {
          break
        }
        a"""
            )
        }
    }

    @Test
    fun `While loop`() {
        vm.execute(
            """a = 12;while(a > 0) {
            a = a - 1;
        }
        a"""
        ).also {
            assertEquals(0L, it)
        }
    }

    @Test
    fun `While loop with break`() {
        vm.execute(
            """a = 12;while(a > 0) {
            a = a - 1;
            if(a < 5) {
                a = 5;
                break;
            }
        }
        a"""
        ).also {
            assertEquals(5L, it)
        }
    }

    @Test
    fun `For loop`() {
        vm.execute(
            """s = 0;for a = 0 till 100 {
            s = a;
        }
        s"""
        ).also {
            assertEquals(100L, it)
        }
    }

    @Test
    fun `For loop with retaining`() {
        vm.execute(
            """s = for a = 0 till 100 {
            a
        }
        s"""
        ).also {
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals(101, it.size)
            assertEquals(100L, it[100])
        }
    }

    @Test
    fun `For loop with continue`() {
        vm.execute(
            """s = 0;for a = 0 till 100 {
            if (a == 100){
                continue;
            }
            s = a;
        }
        s"""
        ).also {
            assertEquals(99L, it)
        }
    }

    @Test
    fun `For loop with continue in retain mode`() {
        vm.execute(
            """s = for a = 0 till 100 {
            if (a == 100){
                continue;
            }
            a
        }
        s"""
        ).also {
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals(100, it.size)
            assertEquals(99L, it[99])
        }
    }

    @Test
    fun `For loop with continue advance`() {
        vm.execute(
            """s = 0;for a = 0 till 100 {
            s = a;
            if (a == 100){
                continue;
            }
        }
        s"""
        ).also {
            assertEquals(100L, it)
        }
    }

    @Test
    fun `For loop advanced`() {
        vm.execute(
            """s = 0;for a = 0 till 100 up with 2{
            s = s + 1;
        }
        s"""
        ).also {
            assertEquals(51L, it)
        }
        vm.execute(
            """<bm>
        s = 0;seginka a = 0 kata 100 kay niin 2 {
            s = s + 1;
        }
        s"""
        ).also {
            assertEquals(51L, it)
        }
        vm.execute(
            """<fr>
        s = 0;pour a = 0 jusqua 100 mont avec 2 {
            s = s + 1;
        }
        s"""
        ).also {
            assertEquals(51L, it)
        }
    }

    @Test
    fun `For of `() {
        vm.execute(
            """
        b = null;
        for a in ["molo", "test"]{
            b = a
        }
        b"""
        ).also {
            assertEquals("test", it)
        }
    }

    @Test
    fun `For of with retaining`() {
        vm.execute(
            """
        b = for a in ["molo", "test"]{
            a
        }
        b"""
        ).also {
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals(2, it.size)
            assertEquals("test", it[1])
        }
    }

    @Test
    fun `For of continue with retaining`() {
        vm.execute(
            """
        b = for a in ["molo", "test", "2", "4"]{
            if(a == "2") break;
            a
        }
        b"""
        ).also {
            assertInstanceOf(List::class.java, it)
            it as List<*>
            assertEquals(2, it.size)
            assertEquals("test", it[1])
        }
    }
}