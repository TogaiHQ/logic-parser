package com.togai.logicparser

data class ValidationResponse(val status: Boolean, val message: String? = null)

data class Attribute(val name: String, val unit: String? = null)

data class Dimension(val name: String )

data class AttributeValue(val name: String, val value: String, val unit: String? = null)

data class DimensionValue(val name: String, val value: String)
