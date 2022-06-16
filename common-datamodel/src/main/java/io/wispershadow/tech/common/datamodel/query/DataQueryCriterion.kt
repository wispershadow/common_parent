package io.wispershadow.tech.common.datamodel.query

interface DataQueryCriterion {
    fun buildQueryString(parameterValues: MutableList<Any?>): String
}

class SimpleDataQueryCriterion(
    val tableName: String? = null,
    val columnName: String,
    val operator: DataQueryOperator,
    val value: Any?
): DataQueryCriterion {

    override fun buildQueryString(parameterValues: MutableList<Any?>): String {
        val queryBuilder = StringBuilder()
        if (tableName != null) {
            queryBuilder.append(tableName).append(".")
        }
        queryBuilder.append(columnName).append(" ")
        queryBuilder.append(operator.operatorValue).append(" ")
        if (operator == DataQueryOperator.IN) {
            queryBuilder.append("(")
            if (value is Iterable<*>) {
                value.forEachIndexed { index, sv ->
                    if (index > 0) {
                        queryBuilder.append(",")
                    }
                    queryBuilder.append("?")
                    parameterValues.add(sv)
                }
                if (parameterValues.isEmpty()) {
                    throw IllegalArgumentException("Illegal in query with empty list")
                }
            }
            else {
                queryBuilder.append("?")
                parameterValues.add(value)
            }
            queryBuilder.append(")")
        }
        else if (operator == DataQueryOperator.LIKE) {
            queryBuilder.append("?")
            parameterValues.add("${value.toString()}%")
        }
        else if (operator == DataQueryOperator.BETWEEN) {
            if (value is List<*> || value is Pair<*, *>) {
                queryBuilder.append("? and ?")
                if (value is List<*>) {
                    if (value.size != 2) {
                        throw IllegalArgumentException("Parameter value must be a list containing 2 values")
                    }
                    parameterValues.add(value[0])
                    parameterValues.add(value[1])
                }
                else if (value is Pair<*, *>) {
                    parameterValues.add(value.first)
                    parameterValues.add(value.second)
                }
            }
            else {
                throw IllegalArgumentException("Parameter value must be list or pair for between operator")
            }

        }
        else {
            if (value == null) {
                if (operator == DataQueryOperator.EQ) {
                    val lastInd = queryBuilder.lastIndexOf(DataQueryOperator.EQ.operatorValue)
                    queryBuilder.deleteRange(lastInd, queryBuilder.length)
                    queryBuilder.append("IS NULL")
                }
                else if (operator == DataQueryOperator.NE) {
                    val lastInd = queryBuilder.lastIndexOf(DataQueryOperator.NE.operatorValue)
                    queryBuilder.deleteRange(lastInd, queryBuilder.length)
                    queryBuilder.append("IS NOT NULL")
                }
                else {
                    throw IllegalArgumentException("Query does not allow null value for operator ${operator}")
                }
            }
            else {
                queryBuilder.append("?")
                parameterValues.add(value)
            }
        }
        return queryBuilder.toString()
    }
}

class LogicalDataQueryCriterion (
    val subDataQueryCriteria: List<DataQueryCriterion>,
    val operator: LogicalOperator): DataQueryCriterion {

    override fun buildQueryString(parameterValues: MutableList<Any?>): String {
        val queryBuilder = StringBuilder()
        if (subDataQueryCriteria.isEmpty()) {
            throw IllegalArgumentException("Illegal logic query with empty conditions")
        }
        subDataQueryCriteria.forEachIndexed {index, dataQueryCriterion ->
            if (index > 0) {
                queryBuilder.append(" ").append(operator.operatorValue).append(" ")
            }
            queryBuilder.append("(").append(dataQueryCriterion.buildQueryString(parameterValues)).append(")")
        }
        return queryBuilder.toString()
    }
}

enum class DataQueryOperator(val operatorValue: String) {
    EQ("="),
    NE("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    LIKE("like"),
    IN("in"),
    BETWEEN("between")
}

enum class LogicalOperator(val operatorValue: String) {
    AND("AND"),
    OR("OR")
}