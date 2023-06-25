package io.wispershadow.tech.common.datamodel.schema.helper

import com.fasterxml.jackson.databind.ObjectMapper
import io.wispershadow.tech.common.datamodel.schema.Column
import io.wispershadow.tech.common.datamodel.schema.ColumnType
import io.wispershadow.tech.common.datamodel.schema.Schema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.PropertyAccessorFactory
import java.math.BigDecimal

class SchemaClassGeneratorTest {
    @Test
    fun testSchemaClassGeneration() {
        val schemaClassGenerator = SchemaClassGenerator()
        val schema = Schema().apply {
            this.name = "testObject"
            this.version = 1L
        }
        schema.addColumn(Column.of("paymentId", ColumnType.STRING, true))
        schema.addColumn(Column.of("payerAmountUsd", ColumnType.DECIMAL, true))
        schema.addColumn(Column.of("payerCountry", ColumnType.STRING, true))
        schema.addColumn(Column.of("payerAge", ColumnType.INT, true))

        val schemaClass = schemaClassGenerator.getSchemaClass(schema, SchemaClassGenerator::class.java.classLoader)
        Assertions.assertEquals(schemaClass.name, "TestObjectV1")
        Assertions.assertEquals(schemaClass.declaredMethods.size, 8)
        val paymentData = "{\"paymentId\": \"1211212121\", \"payerAmountUsd\": 311.123, \"payerCountry\": \"US\", \"payerAge\": 10}"
        val objectMapper = ObjectMapper()
        val data = objectMapper.readValue(paymentData, schemaClass)
        Assertions.assertEquals(getByPropertyName(data, "paymentId"), "1211212121")
        Assertions.assertEquals(getByPropertyName(data, "payerAmountUsd"), BigDecimal("311.123"))
        Assertions.assertEquals(getByPropertyName(data, "payerCountry"), "US")
        Assertions.assertEquals(getByPropertyName(data, "payerAge"), 10)

        val sameSchemaClass = schemaClassGenerator.getSchemaClass(schema, SchemaClassGenerator::class.java.classLoader)
        Assertions.assertEquals(sameSchemaClass.name, "TestObjectV1")
    }

    private fun getByPropertyName(data: Any, propertyName: String): Any? {
        return PropertyAccessorFactory.forBeanPropertyAccess(data).getPropertyValue(propertyName)
    }
}