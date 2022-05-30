package com.togai.logicparser

/**
 * Response for validating rule
 */
data class ValidationResponse(val status: Boolean, val message: String? = null)

/**
 * Attribute used to validate rule
 */
data class Attribute(val name: String, val unit: String? = null)

/**
 * Dimension used to validate rule
 */
data class Dimension(val name: String )

/**
 * Attribute along with value for evaluating rule
 */
data class AttributeValue(val name: String, val value: String, val unit: String? = null)

/**
 * Dimension along with value for evaluating rule
 */
data class DimensionValue(val name: String, val value: String)
