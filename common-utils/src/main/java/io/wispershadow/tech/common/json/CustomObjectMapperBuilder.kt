package io.wispershadow.tech.common.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.wispershadow.tech.common.config.JsonDeserializeConfig
import io.wispershadow.tech.common.config.JsonPropertiesInclusionConfig
import io.wispershadow.tech.common.config.JsonSerializeConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Reference: https://stackoverflow.com/questions/15022617/de-serialize-bean-in-a-custom-way-at-runtime/15051887
class CustomObjectMapperBuilder(
    private var jsonSerializeConfig: JsonSerializeConfig? = null,
    private var jsonDeserializeConfig: JsonDeserializeConfig? = null
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CustomObjectMapperBuilder::class.java)
    }

    fun withSerializeConfig(jsonSerializeConfig: JsonSerializeConfig) = apply {
        this.jsonSerializeConfig = jsonSerializeConfig
    }

    fun withDeserializeConfig(jsonDeserializeConfig: JsonDeserializeConfig) = apply {
        this.jsonDeserializeConfig = jsonDeserializeConfig
    }

    fun build(): ObjectMapper {
        val serializerFactory = BeanSerializerFactory.instance
            .withSerializerModifier(CustomizeBeanSerializerModifier(jsonSerializeConfig))
        val deserializerFactory = BeanDeserializerFactory.instance
            .withDeserializerModifier(CustomizeBeanDeserializeModifier(jsonDeserializeConfig))
        val deserializerContext = DefaultDeserializationContext.Impl(deserializerFactory)
        return ObjectMapper(null, null, deserializerContext).apply {
            this.serializerFactory = serializerFactory
            this.registerModule(JavaTimeModule())
            this.registerKotlinModule()
            this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    /*
        alternative way to exclude properties: however, this has a limitation that the mixIn class
        has to be defined in advance

        val myFilter = SimpleBeanPropertyFilter.filterOutAllExcept("property1", "property2")
        val simpleFilterProvider = SimpleFilterProvider()
            .addFilter("myFilter", myFilter)
        this.setFilterProvider(simpleFilterProvider)
            this.addMixIn(MyBean::class.java, MyMixin::class.java)

        @JsonFilter("myFilter")
        class MyMixin {
        }
    */
    class CustomizeBeanSerializerModifier(val jsonSerializeConfig: JsonSerializeConfig?): BeanSerializerModifier() {
        companion object {
            private val logger: Logger = LoggerFactory.getLogger(CustomizeBeanSerializerModifier::class.java)
        }

        override fun updateBuilder(
            config: SerializationConfig,
            beanDesc: BeanDescription,
            builder: BeanSerializerBuilder
        ): BeanSerializerBuilder {
            jsonSerializeConfig?.let {
                val allClassPropsInclude = mutableMapOf<String, MutableSet<String>>()
                val allClassPropsExclude = mutableMapOf<String, MutableSet<String>>()
                it.propertiesInclusion.forEach { propertyInclusion ->
                    val className = propertyInclusion.className
                    val propertyNames = propertyInclusion.propertyNames
                    if (propertyInclusion.inclusionType == JsonPropertiesInclusionConfig.TYPE_INCLUDE) {
                        allClassPropsInclude.computeIfAbsent(className) {
                            mutableSetOf()
                        }.addAll(propertyNames)
                    }
                    else if (propertyInclusion.inclusionType == JsonPropertiesInclusionConfig.TYPE_EXCLUDE) {
                        allClassPropsExclude.computeIfAbsent(className) {
                            mutableSetOf()
                        }.addAll(propertyNames)
                    }
                }

                val currentBeanClassName = beanDesc.beanClass.name
                if (allClassPropsInclude.containsKey(currentBeanClassName)) {
                    val includedProperties = allClassPropsInclude.getValue(currentBeanClassName)
                    logger.debug("Getting list of included properties for beanClass: {}, property names are : {}",
                    currentBeanClassName, includedProperties)
                    val beanProperties = builder.properties
                    val propertyIterator = beanProperties.iterator()
                    while (propertyIterator.hasNext()) {
                        val propertyName = propertyIterator.next().name
                        if (!includedProperties.contains(propertyName)) {
                            logger.debug("Remove irrelevant property: {}", propertyName)
                            propertyIterator.remove()
                        }
                    }
                }
                else if (allClassPropsExclude.containsKey(currentBeanClassName)) {
                    val excludedProperties = allClassPropsExclude.getValue(currentBeanClassName)
                    logger.debug("Getting list of excluded properties for beanClass: {}, property names are : {}",
                        currentBeanClassName, excludedProperties)
                    val beanProperties = builder.properties
                    val propertyIterator = beanProperties.iterator()
                    while (propertyIterator.hasNext()) {
                        val propertyName = propertyIterator.next().name
                        if (excludedProperties.contains(propertyName)) {
                            logger.debug("Remove irrelevant property: {}", propertyName)
                            propertyIterator.remove()
                        }
                    }
                }
            }
            return builder
        }
    }

    class CustomizeBeanDeserializeModifier(jsonDeserializeConfig: JsonDeserializeConfig?): BeanDeserializerModifier() {

    }
}