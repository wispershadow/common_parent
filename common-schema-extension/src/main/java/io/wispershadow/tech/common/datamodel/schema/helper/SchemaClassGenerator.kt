package io.wispershadow.tech.common.datamodel.schema.helper

import io.wispershadow.tech.common.datamodel.schema.ColumnType
import io.wispershadow.tech.common.datamodel.schema.Schema
import io.wispershadow.tech.common.utils.LockTemplate
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FieldAccessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class SchemaClassGenerator {
    private val schemaLock: ReadWriteLock = ReentrantReadWriteLock()
    private val schemaClassCache = mutableMapOf<String, Class<*>>()
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SchemaClassGenerator::class.java)

        private val columnTypeToJavaClassMapping = mapOf<ColumnType, Class<*>>(
            ColumnType.STRING to String::class.java,
            ColumnType.INT to Int::class.java,
            ColumnType.LONG to Long::class.java,
            ColumnType.DECIMAL to BigDecimal::class.java,
            ColumnType.DATE to Date::class.java,
            ColumnType.TIMESTAMP to Timestamp::class.java
        )
    }

    fun getSchemaClass(schema: Schema, classLoader: ClassLoader): Class<*> {
        val key = "${schema.name}_${schema.version}"
        return LockTemplate.lockForCacheLoad(schemaLock, key, schemaClassCache, { _ -> buildSchemaClass(schema, classLoader)})!!
    }


    private fun buildSchemaClass(schema: Schema, classLoader: ClassLoader): Class<*> {
        logger.info("Start building schema class for schema={}, version={}", schema.name, schema.version)
        val typeBuilder = ByteBuddy().subclass(Any::class.java).name(className(schema))
        var tempBuilder = typeBuilder
        schema.columns.forEach { column ->
            tempBuilder = tempBuilder
                .defineField(column.name, columnTypeToJavaClassMapping[column.type], Visibility.PRIVATE)
        }
        schema.columns.forEach { column ->
            tempBuilder = tempBuilder.defineMethod(toGet(column.name), columnTypeToJavaClassMapping[column.type], Modifier.PUBLIC)
                .intercept(FieldAccessor.ofBeanProperty())
                .defineMethod(toSet(column.name), Void.TYPE, Modifier.PUBLIC).withParameters(columnTypeToJavaClassMapping[column.type])
                .intercept(FieldAccessor.ofBeanProperty())
        }
        return tempBuilder.make().load(classLoader, ClassLoadingStrategy.Default.WRAPPER).loaded.also {
            logger.info("Success building schema class: {}", it.name)
        }
    }

    private fun toGet(name: String): String {
        return "get${name[0].toUpperCase()}${name.substring(1)}"
    }

    private fun toSet(name: String): String {
        return "set${name[0].toUpperCase()}${name.substring(1)}"
    }

    private fun className(schema: Schema): String {
        val schemaName = schema.name
        return "${schemaName[0].toUpperCase()}${schemaName.substring(1)}V${schema.version}"
    }
}