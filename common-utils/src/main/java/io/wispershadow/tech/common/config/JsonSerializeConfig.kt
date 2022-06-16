package io.wispershadow.tech.common.config

class JsonSerializeConfig {
    var propertiesInclusion: List<JsonPropertiesInclusionConfig> = emptyList()
    var propertiesWithCustomDateFormat: List<CustomDateFormatConfig> = emptyList()

}

class JsonPropertiesInclusionConfig {
    companion object {
        val TYPE_INCLUDE = 1
        val TYPE_EXCLUDE = 2
    }
    lateinit var className: String
    var propertyNames: List<String> = emptyList()
    var inclusionType: Int = TYPE_INCLUDE
}

class CustomDateFormatConfig {
    lateinit var className: String
    var propertyNames: List<String> = emptyList()
    lateinit var dateFormat: String
}