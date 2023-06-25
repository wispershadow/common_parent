package io.wispershadow.tech.common.el

import org.apache.commons.collections.map.LRUMap
import org.apache.commons.lang.StringUtils
import org.springframework.expression.Expression
import org.springframework.expression.spel.SpelNode
import org.springframework.expression.spel.ast.VariableReference
import org.springframework.expression.spel.standard.SpelExpression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.util.concurrent.locks.ReentrantReadWriteLock

class SpringExpressionEvaluator: ExpressionEvaluator {
    private val readWriteLock = ReentrantReadWriteLock()
    private val expressionCache = LRUMap(500)

    override fun getVariables(expressionStr: String): Set<String> {
        val expression = getOrParseExpression(expressionStr)
        val result = mutableSetOf<String>()
        getVariables((expression as SpelExpression).ast, result)
        return result
    }

    override fun <R> evaluateExpression(expressionStr: String, variables: Map<String, Any>, resultClass: Class<R>): R? {
        val expression = getOrParseExpression(expressionStr)
        val context = StandardEvaluationContext()
        variables.forEach { (k, v) -> context.setVariable(k, v) }
        return expression.getValue(context, resultClass)
    }

    override fun <R> evaluateExpression(expressionStr: String, rootObject: Any, variables: Map<String, Any>, resultClass: Class<R>): R? {
        val expression = getOrParseExpression(expressionStr)
        val context = StandardEvaluationContext()
        context.setRootObject(rootObject)
        variables.forEach { (k, v) -> context.setVariable(k, v) }
        return expression.getValue(context, resultClass)
    }

    override fun reset() {
        readWriteLock.writeLock().lock()
        try {
            expressionCache.clear()
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    private fun getOrParseExpression(expressionString: String): Expression {
        var expression: Expression?
        readWriteLock.readLock().lock()
        try {
            expression = expressionCache[expressionString] as Expression?
            if (expression == null) {
                readWriteLock.readLock().unlock()
                readWriteLock.writeLock().lock()
                try {
                    expression = doParseExpression(expressionString)
                    expressionCache[expressionString] = expression
                } finally {
                    readWriteLock.readLock().lock()
                    readWriteLock.writeLock().unlock()
                }
            }
        } finally {
            readWriteLock.readLock().unlock()
        }
        return expression!!
    }

    private fun doParseExpression(expressionString: String): Expression {
        val parser = SpelExpressionParser()
        return parser.parseExpression(expressionString)
    }

    private fun getVariables(node: SpelNode?, result: MutableSet<String>) {
        node?.let {
            if (it is VariableReference) {
                result.add(StringUtils.remove(node.toStringAST(), "#"))
            }
            for (i in 0 until node.childCount) {
                var child = node.getChild(i)
                getVariables(child, result)
            }
        }
    }
}