package io.wispershadow.tech.common.datamodel.schema.impl

import io.wispershadow.tech.common.datamodel.schema.Column
import io.wispershadow.tech.common.datamodel.schema.ColumnType
import io.wispershadow.tech.common.datamodel.schema.Schema
import io.wispershadow.tech.common.datamodel.schema.SchemaGenerator
import io.wispershadow.tech.common.utils.LockTemplate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Types
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.sql.DataSource

class DBSchemaGenerator(val dataSource: DataSource,
                        val databaseName: String): SchemaGenerator {
    private val cachedSchema: MutableMap<String, Schema> = mutableMapOf()
    private val readWriteLock = ReentrantReadWriteLock(true)
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DBSchemaGenerator::class.java)
    }

    override fun generateSchema(schemaName: String): Schema {
        return LockTemplate.lockForCacheLoad(readWriteLock, schemaName,
            cachedSchema, this::loadSchemaFromDataSourceMetadata)!!
    }

    private fun loadSchemaFromDataSourceMetadata(tableName: String): Schema {
        logger.info("Start generating schema for database: {}, table: {}", databaseName, tableName)
        val connection = dataSource.connection
        val primaryKeyColumns = mutableListOf<String>()
        val columnList = mutableListOf<Column>()
        connection.use { connection ->
            val databaseMetadata = connection.metaData
            val primaryKeysResultSet = databaseMetadata.getPrimaryKeys(databaseName, null, tableName)
            primaryKeysResultSet.use { primaryKeysResultSet ->
                while (primaryKeysResultSet.next()) {
                    val columnName = primaryKeysResultSet.getString("COLUMN_NAME")
                    primaryKeyColumns.add(columnName)
                }
            }
            val columnsResultSet = databaseMetadata.getColumns(databaseName, null, tableName, null)
            columnsResultSet.use { columnsResultSet ->
                while (columnsResultSet.next()) {
                    val columnName = columnsResultSet.getString("COLUMN_NAME")
                    val columnSize = columnsResultSet.getString("COLUMN_SIZE")
                    val dataTypeInt = columnsResultSet.getInt("DATA_TYPE")
                    val isNullable = columnsResultSet.getString("IS_NULLABLE")
                    columnList.add(Column.of(
                        name = columnName,
                        type = mapColumnType(dataTypeInt),
                        key = primaryKeyColumns.contains(columnName),
                        nullable = ("YES" == isNullable)))
                }
            }
        }
        return Schema.of(tableName, 1L, columnList)
    }

    private fun mapColumnType(dataTypeInt: Int): ColumnType {
        when (dataTypeInt) {
            Types.VARCHAR -> {
                return ColumnType.STRING
            }
            Types.INTEGER -> {
                return ColumnType.INT
            }
            Types.BIGINT -> {
                return ColumnType.LONG
            }
            Types.DATE -> {
                return ColumnType.DATE
            }
            Types.TIMESTAMP -> {
                return ColumnType.TIMESTAMP
            }
            Types.DECIMAL -> {
                return ColumnType.DECIMAL
            }
            Types.CHAR -> {
                return ColumnType.STRING
            }
            else -> {
                throw IllegalArgumentException("Unable to map type ${dataTypeInt}")
            }
        }
    }

}