package io.wispershadow.tech.common.datamodel.schema

interface SchemaGenerator {
    fun generateSchema(schemaName: String): Schema
}