package io.wispershadow.tech.common.datamodel.schema.helper

import io.wispershadow.tech.common.datamodel.schema.Schema

object SqlBuilderFromSchema {
    fun buildSelectQuery(schema: Schema): String {
        val queryBuilder = StringBuilder("SELECT ")
        val columnDefinitions = schema.columns.joinToString(separator = ",", transform = {column -> column.name})
        queryBuilder.append(columnDefinitions)
        queryBuilder.append(" FROM ").append(schema.name)
        return queryBuilder.toString()
    }

    fun buildSelectQueryWithKey(schema: Schema): String {
        val queryBuilder = StringBuilder("SELECT ")
        val columnDefinitions = schema.columns.joinToString(separator = ",", transform = {column -> column.name})
        queryBuilder.append(columnDefinitions)
        queryBuilder.append(" FROM ").append(schema.name).append(" WHERE ")
        val keyCondition = schema.columns.filter { column -> column.key }.joinToString(separator = " AND ", transform = {column -> "${column.name} = ?" })
        queryBuilder.append(keyCondition)
        return queryBuilder.toString()
    }

    fun buildInsertQuery(schema: Schema): String {
        val queryBuilder = StringBuilder("INSERT INTO ${schema.name} ")
        val columnDefinitions = schema.columns.joinToString (separator = ",", prefix = "(",  postfix = ")", transform = {column -> column.name})
        queryBuilder.append(columnDefinitions)
        queryBuilder.append(" VALUES ")
        val parameters = schema.columns.joinToString(separator = ",", prefix = "(", postfix = ")", transform = {column -> "?"})
        queryBuilder.append(parameters)
        return queryBuilder.toString()
    }

    fun buildUpsertQuery(schema: Schema, columnsToUpdate: List<String> = emptyList()): String {
        val insertQuery = buildInsertQuery(schema)
        val queryBuilder = StringBuilder(insertQuery)
        queryBuilder.append(" ON DUPLICATE KEY UPDATE ")
        queryBuilder.append(buildUpdateColList(schema, columnsToUpdate))
        return queryBuilder.toString()
    }

    fun buildUpdateQuery(schema: Schema, columnsToUpdate: List<String> = emptyList()): String {
        val queryBuilder = StringBuilder("UPDATE ${schema.name} SET ")
        val columnListSegment = buildUpdateColList(schema, columnsToUpdate)
        queryBuilder.append(columnListSegment)
        queryBuilder.append(" WHERE ")
        val keyCondition = schema.columns.filter { column -> column.key }.joinToString(separator = " AND ", transform = {column -> "${column.name} = ?" })
        queryBuilder.append(keyCondition)
        return queryBuilder.toString()
    }

    private fun buildUpdateColList(schema: Schema, columnsToUpdate: List<String>): String {
        val columnNameList: List<String> = if (columnsToUpdate.isNotEmpty()) {
            columnsToUpdate
        }
        else {
            schema.columns.filter { column -> !column.key
            }.map { column ->
                column.name
            }
        }
        return columnNameList.map { columnName ->
            "${columnName} = ?"
        }.joinToString(",")
    }
}