package io.wispershadow.tech.common.utils

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import java.util.regex.Pattern

object PropertyUtils {
    private val listPattern = Pattern.compile("(\\[[0-9]+\\])")

    fun getPropertyValue(source: Any?, propertyName: String): Any? {
        return source?.let {
            when (source) {
                is BeanWrapper -> source.getPropertyValue(propertyName)
                is Map<*, *> -> {
                    getMapProperty(propertyName, source)
                }
                else -> BeanWrapperImpl(source).getPropertyValue(propertyName)
            }
        }
    }

    private fun getMapProperty(propertyName: String, propertyMap: Map<*, *>): Any? {
        return if (propertyName.indexOf('.') >= 0) {
            val simplePropName = propertyName.substringBefore('.')
            val suffix = propertyName.substringAfter(".")
            val propValue = getMapPropertySimple(simplePropName, propertyMap)
            if (propValue != null) {
                getPropertyValue(propValue, suffix)
            } else {
                null
            }
        } else {
            getMapPropertySimple(propertyName, propertyMap)
        }
    }


    private fun getMapPropertySimple(propertyName: String, propertyMap: Map<*, *>): Any? {
        val listIndices = getListIndices(propertyName)
        val propertyNamePrefix = listIndices.first
        val indicesValues = listIndices.second
        if (indicesValues.isEmpty()) {
            return propertyMap[propertyNamePrefix]
        }
        var propValue = propertyMap[propertyNamePrefix]
        indicesValues.forEach { index ->
            if (propValue !is List<*>) {
                throw RuntimeException("Value is not a list")
            }
            propValue = (propValue as List<*>)[index]
        }
        return propValue
    }

    private fun getListIndices(propertyName: String): Pair<String, List<Int>> {
        val matcher = listPattern.matcher(propertyName)
        val matchedPatternList = mutableListOf<Array<Int>>()
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val group = matcher.group()
            val indexValue = group.substring(1, group.length - 1).toInt()
            matchedPatternList.add(arrayOf(start, end, indexValue))
        }
        var validIndices = true
        if (matchedPatternList.isNotEmpty()) {
            //check no additional elements after last [] in property name
            val lastMatchedPattern = matchedPatternList[matchedPatternList.size - 1]
            if (lastMatchedPattern[1] < propertyName.length - 1) {
                validIndices = false
            }
            else {
                //check no gap between each []
                for (i in 0 until matchedPatternList.size - 1) {
                    if (matchedPatternList[i+1][0] - matchedPatternList[i][1] > 1) {
                        validIndices = false
                        break
                    }
                }
            }
        }

        return if (validIndices) {
            val propertyNamePrefix = if (matchedPatternList.isNotEmpty()) {
                propertyName.substring(0, matchedPatternList[0][0])
            }
            else {
                propertyName
            }
            Pair(propertyNamePrefix, matchedPatternList.map{ it[2] })
        } else {
            Pair(propertyName, emptyList())
        }
    }




    fun setPropertyValue(source: Any?, propertyName: String, propertyValue: Any?) {
        source?.let {
            when (source) {
                is BeanWrapper -> source.setPropertyValue(propertyName, propertyValue)
                is MutableMap<*, *> -> {
                    val sourceMap = source as MutableMap<String, Any?>
                    sourceMap[propertyName] = propertyValue
                }
                else -> BeanWrapperImpl(source).setPropertyValue(propertyName, propertyValue)
            }
        }
    }

}