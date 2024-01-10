/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.togai.logicparser

import io.github.jamsesso.jsonlogic.JsonLogic
import io.github.jamsesso.jsonlogic.JsonLogicException
import io.github.jamsesso.jsonlogic.ast.JsonLogicArray
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser
import io.github.jamsesso.jsonlogic.ast.JsonLogicString
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression
import java.util.Queue
import java.util.LinkedList

class LogicParser {
    companion object {
        const val DIMENSIONS = "dimensions"
        const val ATTRIBUTES = "attributes"
        const val DEPENDENCIES = "dependencies"
        const val USAGE = "usage"
        const val REVENUE = "revenue"
        const val USAGE_TAG = "usage_tag"
        const val REVENUE_TAG = "revenue_tag"
    }

    private val jsonLogic =  JsonLogic().addCache(LRUCache())
    private val expressionNames = jsonLogic.expressions.map(JsonLogicExpression::key).toMutableSet()

    /**
     * Method to add custom operations
     * @param name Name of the function
     * @param function Function to execute
     */
    fun addOperation(name: String, function: (Array<Any>) -> Any) {
        jsonLogic.addOperation(name, function)
        expressionNames.add(name)
    }

    /**
     * Method to validate whether the rule is valid or not.
     * @param rule Rule in json format to validate
     * @param attributes List of Variable instance
     * @param dimensions List of Variable instance
     * @param dependencies List of Variable instance
     * @return ValidationResponse
     **/
    fun validateExpression(
        rule: String,
        attributes: List<Variable>,
        dimensions: List<Variable>,
        dependencies: List<Variable>? = null
    ): ValidationResponse {
        val variables = HashSet<String>()

        attributes.forEach {
            variables.add("$ATTRIBUTES.${it.name}")
        }
        dimensions.forEach {
            variables.add("$DIMENSIONS.${it.name}")
        }
        dependencies?.forEach {
            variables.add("$DEPENDENCIES.${it.name}")
        }

        return validateExpression(rule, variables)
    }

    fun validateExpression(
        rule: String,
        billableIds: List<String>,
        tags: List<String>,
    ): ValidationResponse {
        val variables = HashSet<String>()

        billableIds.forEach {
            variables.add("$USAGE.$it")
            variables.add("$REVENUE.$it")
        }
        tags.forEach {
            variables.add("$USAGE_TAG.$it")
            variables.add("$REVENUE_TAG.$it")
        }

        return validateExpression(rule, variables)
    }

    /**
     * Method to validate whether the rule given set of params.
     * @param rule Rule in json format to validate
     * @param variables List of Strings
     * @return ValidationResponse
     **/
    private fun validateExpression(rule: String, variables: Set<String>): ValidationResponse {
        try {
            val node = JsonLogicParser.parse(rule)
            traverseNode(node, variables)
        } catch (e: JsonLogicException) {
            return ValidationResponse(false, e.message)
        }
        return ValidationResponse(true)
    }

    /**
     * Method to evaluate the rule using the given data.
     * @param rule Rule in json format to evaluate
     * @param attributeValues List of Value instance
     * @param dimensionValues List of Value instance
     * @param dependencyValues List of Value instance
     * @return The response of evaluation
     **/
    @OptIn(ExperimentalStdlibApi::class)
    fun evaluateExpression(
        rule: String,
        attributeValues: List<Value>,
        dimensionValues: List<Value>,
        dependencyValues: List<Value>? = null,
    ): Any? {
        val data: Map<String, Map<String, String>> = buildMap {
            put(ATTRIBUTES, attributeValues.associate { it.name to it.value })
            put(DIMENSIONS, dimensionValues.associate { it.name to it.value })
            put(DEPENDENCIES, dependencyValues?.associate { it.name to it.value } ?: emptyMap())
        }
        return jsonLogic.apply(rule, data)
    }

    fun evaluateExpression(
        rule: String,
        data: Map<String, Value>
    ): Any? = jsonLogic.apply(rule, data)


    fun evaluateExpressionWithMapData(
        rule: String,
        data: Map<String, String>
    ): Any? = jsonLogic.apply(rule, data)

    fun truthy(
        rule: String,
        attributeValues: List<Value>,
        dimensionValues: List<Value>
    ): Boolean {
        return JsonLogic.truthy(evaluateExpression(rule, attributeValues, dimensionValues))
    }

    private fun traverseNode(rootNode: JsonLogicNode, variables: Set<String>) {
        val nodes: Queue<JsonLogicNode> = LinkedList()
        nodes.add(rootNode)
        while (nodes.isNotEmpty()) {
            when (val node = nodes.poll()) {
                is JsonLogicVariable -> {
                    if (node.key !is JsonLogicString) {
                        throw JsonLogicException("Variable name must be a string")
                    }
                    val variableName = (node.key as JsonLogicString).value
                    if (!variables.contains(variableName)) {
                        throw JsonLogicException("Unknown variable: $variableName")
                    }
                }
                is JsonLogicArray -> {
                    node.forEach {
                        nodes.add(it)
                    }
                }
                is JsonLogicOperation -> {
                    if (!expressionNames.contains(node.operator)) {
                        throw JsonLogicException("Unknown operator/operation: ${node.operator}")
                    }
                    node.arguments.forEach {
                        nodes.add(it)
                    }
                }
            }
        }
    }
}

