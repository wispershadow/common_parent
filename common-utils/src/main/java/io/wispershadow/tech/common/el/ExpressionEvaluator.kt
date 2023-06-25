package io.wispershadow.tech.common.el

interface ExpressionEvaluator {
    fun getVariables(expressionStr: String): Set<String>

    fun <R> evaluateExpression(expressionStr: String, variables: Map<String, Any>, resultClass: Class<R>): R?

    fun <R> evaluateExpression(expressionStr: String, rootObject: Any, variables: Map<String, Any>, resultClass: Class<R>): R?

    fun reset()
}