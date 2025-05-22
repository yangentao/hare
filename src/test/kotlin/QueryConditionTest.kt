import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.utils.*
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.pool.HarePool
import io.github.yangentao.sql.pool.LiteSources
import io.github.yangentao.types.printX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConA : TableModel() {
    @ModelField
    var a: String by model

    @ModelField
    var b: String by model

    @ModelField
    var c: String by model

    @ModelField
    var d: String by model

    companion object : TableModelClass<ConA>()
}

class QueryConditionTest {
    @Test
    fun c() {
        HarePool.pushSource(LiteSources.sqliteMemory())
        val w = ConA.queryConditions("a|EQ|1, b|start|bb")
        printX("Where: ", w?.sql)
        // cona.a = ? AND cona.b LIKE 'bb%'
    }

    @Test
    fun b() {
        val q = "a|EQ|1, [b|GE|2, {c|nul, d|eq|1}, [x|eq|1, y|eq|2]] "
        val p = ConditionParser(q).parse()
        assertTrue(p is AndCond)
        assertEquals(2, p.items.size)
        assertTrue(p.items[0] is FieldCond)
        assertTrue(p.items[1] is OrCond)
    }

    @Test
    fun a() {
        val q = "{a|EQ|1, b|GE|2, c|nul}"
        val p = ConditionParser(q).parse()
        assertTrue(p is AndCond)
        assertEquals(3, p.items.size)
        assertEquals(FieldCond("a", "EQ", listOf("1")), p.items.first())
        assertEquals(FieldCond("b", "GE", listOf("2")), p.items[1])
        assertEquals(FieldCond("c", "nul", listOf()), p.items[2])

    }
}