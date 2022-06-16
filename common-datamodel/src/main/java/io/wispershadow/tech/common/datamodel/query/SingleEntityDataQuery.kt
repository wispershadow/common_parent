package io.wispershadow.tech.common.datamodel.query

class SingleEntityDataQuery {
    lateinit var tableName: String
    var queryConditions: List<DataQueryCriterion> = emptyList()
    var sortConditions: List<Sort> = emptyList()
    var pagination: Pagination? = null
}