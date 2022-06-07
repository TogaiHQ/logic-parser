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
     * @param attributes List of Attribute instance
     * @param dimensions List of Dimension instance
     * @return ValidationResponse
     **/
    fun validateExpression(rule: String, attributes: List<Attribute>, dimensions: List<Dimension>): ValidationResponse {
        val variables = HashSet<String>()

        attributes.forEach {
            variables.add("$ATTRIBUTES.${it.name}")
        }
        dimensions.forEach {
            variables.add("$DIMENSIONS.${it.name}")
        }

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
     * @param attributeValues List of AttributeValue instance
     * @param dimensionValues List of DimensionValue instance
     * @return The response of evaluation
     **/
    fun evaluateExpression(
        rule: String,
        attributeValues: List<AttributeValue>,
        dimensionValues: List<DimensionValue>
    ): Any? {
        val data: HashMap<String, HashMap<String, String>> =
            hashMapOf(ATTRIBUTES to hashMapOf(), DIMENSIONS to hashMapOf())
        for (attributeValue in attributeValues) {
            data[ATTRIBUTES]!![attributeValue.name] = attributeValue.value
        }
        for (dimensionValue in dimensionValues) {
            data[DIMENSIONS]!![dimensionValue.name] = dimensionValue.value
        }
        return jsonLogic.apply(rule, data)
    }

    fun truthy(
        rule: String,
        attributeValues: List<AttributeValue>,
        dimensionValues: List<DimensionValue>
    ): Boolean {
        return JsonLogic.truthy(evaluateExpression(rule, attributeValues, dimensionValues))
    }

    private fun traverseNode(rootNode: JsonLogicNode, variables: HashSet<String>) {
        val nodes: Queue<JsonLogicNode> = LinkedList()
        nodes.add(rootNode)
        while (nodes.isNotEmpty()) {
            when (val node = nodes.poll()) {
                is JsonLogicVariable -> {
                    if (node.key !is JsonLogicString) {
                        throw JsonLogicException("Variable name must be a string")
                    }
                    if (!variables.contains((node.key as JsonLogicString).value)) {
                        throw JsonLogicException("Unknown variable: ${node.key}")
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

