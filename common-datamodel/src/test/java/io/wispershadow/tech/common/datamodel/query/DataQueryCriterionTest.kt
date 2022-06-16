package io.wispershadow.tech.common.datamodel.query

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DataQueryCriterionTest {
    @Test
    fun testBuildSimpleDataQueryWithTableName() {
        val parameterValue = 134L
        val dataQueryCriterion = SimpleDataQueryCriterion(tableName = "T_ORDER",
            columnName = "ORDER_ID", operator = DataQueryOperator.EQ, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "T_ORDER.ORDER_ID = ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }


    @Test
    fun testBuildSimpleDataQueryEq() {
        val parameterValue = 134L
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.EQ, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID = ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryEqNull() {
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.EQ, value = null)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID IS NULL")
        Assertions.assertEquals(parameterList, emptyList<Any>())
    }

    @Test
    fun testBuildSimpleDataQueryNe() {
        val parameterValue = "abc"
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.NE, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID <> ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryNeNull() {
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.NE, value = null)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID IS NOT NULL")
        Assertions.assertEquals(parameterList, emptyList<Any>())
    }

    @Test
    fun testBuildSimpleDataQueryGe() {
        val parameterValue = 103L
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.GE, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID >= ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryGt() {
        val parameterValue = 103L
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.GT, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID > ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryLe() {
        val parameterValue = 103L
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.LE, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID <= ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryLt() {
        val parameterValue = 103L
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.LT, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID < ?")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryIn1() {
        val parameterValue = listOf<String>("abc", "def", "g")
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.IN, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID in (?,?,?)")
        Assertions.assertEquals(parameterList, parameterValue)
    }

    @Test
    fun testBuildSimpleDataQueryIn2() {
        val parameterValue = listOf<String>("abc")
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.IN, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID in (?)")
        Assertions.assertEquals(parameterList, parameterValue)
    }

    @Test
    fun testBuildSimpleDataQueryIn3() {
        val parameterValue = "abc"
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.IN, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID in (?)")
        Assertions.assertEquals(parameterList, listOf(parameterValue))
    }

    @Test
    fun testBuildSimpleDataQueryIn4() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.IN, value = emptyList<String>())
            val parameterList = mutableListOf<Any?>()
            val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        }
    }

    @Test
    fun testBuildSimpleDataQueryLike() {
        val parameterValue = "abc"
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.LIKE, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID like ?")
        Assertions.assertEquals(parameterList, listOf("${parameterValue}%"))
    }

    @Test
    fun testBuildSimpleDataQueryBetween1() {
        val parameterValue = listOf("abc", "edf")
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.BETWEEN, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID between ? and ?")
        Assertions.assertEquals(parameterList, parameterValue)
    }

    @Test
    fun testBuildSimpleDataQueryBetween2() {
        val parameterValue = Pair("abc", "edf")
        val dataQueryCriterion = SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.BETWEEN, value = parameterValue)
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "ORDER_ID between ? and ?")
        Assertions.assertEquals(parameterList, listOf("abc", "edf"))
    }

    @Test
    fun testBuildSimpleDataQueryBetween3() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            val parameterValue = "abc"
            val dataQueryCriterion = SimpleDataQueryCriterion(
                columnName = "ORDER_ID",
                operator = DataQueryOperator.BETWEEN,
                value = parameterValue
            )
            val parameterList = mutableListOf<Any?>()
            val resultQueryString = dataQueryCriterion.buildQueryString(parameterList)
        }
    }

    @Test
    fun testBuildLogicalDataQuery1() {
        val logicalDataQueryCriterion1 = LogicalDataQueryCriterion(
            listOf(
                SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.EQ, value = "abc"),
                SimpleDataQueryCriterion(columnName = "AMOUNT", operator = DataQueryOperator.GT, value = 335.13)
            ),
            LogicalOperator.OR
        )
        val logicalDataQueryCriterion2 = LogicalDataQueryCriterion(
            listOf(
                SimpleDataQueryCriterion(columnName = "STATUS", operator = DataQueryOperator.IN, value = listOf("pending", "active")),
                logicalDataQueryCriterion1
            ),
            LogicalOperator.AND
        )
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = logicalDataQueryCriterion2.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "(STATUS in (?,?)) AND ((ORDER_ID = ?) OR (AMOUNT > ?))")
        Assertions.assertEquals(parameterList, listOf("pending", "active", "abc", 335.13))

    }


    @Test
    fun testBuildLogicalDataQuery2() {
        val logicalDataQueryCriterion = LogicalDataQueryCriterion(
            listOf(
                SimpleDataQueryCriterion(columnName = "ORDER_ID", operator = DataQueryOperator.EQ, value = "abc"),
                SimpleDataQueryCriterion(columnName = "AMOUNT", operator = DataQueryOperator.GT, value = 335.13),
                SimpleDataQueryCriterion(columnName = "STATUS", operator = DataQueryOperator.IN, value = listOf("pending", "active"))
            ),
            LogicalOperator.AND
        )
        val parameterList = mutableListOf<Any?>()
        val resultQueryString = logicalDataQueryCriterion.buildQueryString(parameterList)
        Assertions.assertEquals(resultQueryString, "(ORDER_ID = ?) AND (AMOUNT > ?) AND (STATUS in (?,?))")
        Assertions.assertEquals(parameterList, listOf("abc", 335.13, "pending", "active"))
    }

    @Test
    fun testBuildLogicalDataQuery3() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            val logicalDataQueryCriterion = LogicalDataQueryCriterion(
                emptyList(),
                LogicalOperator.OR
            )
            val parameterList = mutableListOf<Any?>()
            val resultQueryString = logicalDataQueryCriterion.buildQueryString(parameterList)
        }
    }

}