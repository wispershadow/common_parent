package io.wispershadow.tech.common.ratelimit.impl

import io.github.resilience4j.spring6.spelresolver.SpelResolver
import io.github.resilience4j.spring6.spelresolver.SpelRootObject
import org.slf4j.LoggerFactory
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.lang.reflect.Method

class SimpleSpelResolver(val expressionParser: SpelExpressionParser,
                         val parameterNameDiscoverer: ParameterNameDiscoverer
    ): SpelResolver {
    companion object {
        val logger = LoggerFactory.getLogger(SpelResolver::class.java)
    }

    override fun resolve(method: Method, arguments: Array<out Any>, spelExpression: String?): String {
        if (spelExpression == null || spelExpression.isBlank()) {
            return spelExpression!!
        }
        val rootObject = SpelRootObject(method, arguments)
        val evaluationContext =
            MethodBasedEvaluationContext(rootObject, method, arguments, this.parameterNameDiscoverer)
        //try {
            val evaluated = expressionParser.parseExpression(spelExpression).getValue(evaluationContext)
            return evaluated?.toString() ?: spelExpression
        //}
        //catch (ex: Exception) {
            //logger.error("Failed to resolve SpEL expression: $spelExpression", ex)
            //return spelExpression
        //}
    }

}