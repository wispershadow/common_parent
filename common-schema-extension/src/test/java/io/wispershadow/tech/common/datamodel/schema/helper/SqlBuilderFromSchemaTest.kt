package io.wispershadow.tech.common.datamodel.schema.helper

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.wispershadow.tech.common.datamodel.schema.Schema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SqlBuilderFromSchemaTest {
    private val objectMapper = ObjectMapper().apply {
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Test
    fun testBuildInsert() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("simpleSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val insertQuery = SqlBuilderFromSchema.buildInsertQuery(schema)
        Assertions.assertEquals(insertQuery, "INSERT INTO order (id,name,description,amount) VALUES (?,?,?,?)")
    }

    @Test
    fun testBuildUpdate1() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("simpleSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val updateQuery = SqlBuilderFromSchema.buildUpdateQuery(schema)
        Assertions.assertEquals(updateQuery, "UPDATE order SET name = ?,description = ?,amount = ? WHERE id = ?")
    }

    @Test
    fun testBuildUpdate2() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("compositeSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val updateQuery = SqlBuilderFromSchema.buildUpdateQuery(schema)
        Assertions.assertEquals(updateQuery, "UPDATE userorder SET overdue = ? WHERE orderId = ? AND userId = ?")
    }

    @Test
    fun testBuildUpdate3() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("simpleSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val updateQuery = SqlBuilderFromSchema.buildUpdateQuery(schema, listOf("amount"))
        Assertions.assertEquals(updateQuery, "UPDATE order SET amount = ? WHERE id = ?")
    }

    @Test
    fun testBuildUpsert1() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("simpleSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val upsertQuery = SqlBuilderFromSchema.buildUpsertQuery(schema)
        Assertions.assertEquals(upsertQuery, "INSERT INTO order (id,name,description,amount) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE name = ?,description = ?,amount = ?")
    }

    @Test
    fun testBuildUpsert2() {
        val schemaStream = SqlBuilderFromSchemaTest::class.java.classLoader.getResourceAsStream("simpleSchema.json")
        val schema = objectMapper.readValue(schemaStream, Schema::class.java)
        val upsertQuery = SqlBuilderFromSchema.buildUpsertQuery(schema, listOf("amount"))
        Assertions.assertEquals(upsertQuery, "INSERT INTO order (id,name,description,amount) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE amount = ?")
    }
}