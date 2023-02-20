package com.togai.logicparser

/**
 * Response for validating rule
 */
data class ValidationResponse(val status: Boolean, val message: String? = null)

open class Variable(open val name: String)

/**
 * Attribute used to validate rule
 */
data class Attribute(override val name: String, val unit: String? = null): Variable(name)

/**
 * Dimension used to validate rule
 */
data class Dimension(override val name: String ): Variable(name)

open class Value(override val name: String, open val value: String): Variable(name)

/**
 * Attribute along with value for evaluating rule
 */
data class AttributeValue(override val name: String, override val value: String, val unit: String? = null) :
    Value(name, value)

/**
 * Dimension along with value for evaluating rule
 */
data class DimensionValue(override val name: String, override val value: String): Value(name, value)
