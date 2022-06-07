package com.togai.logicparser

import com.google.common.cache.CacheBuilder
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode
import io.github.jamsesso.jsonlogic.cache.JsonLogicCache
import java.util.concurrent.TimeUnit

val CACHE_EXPIRY_TIME_UNIT = TimeUnit.DAYS
const val CACHE_EXPIRY_TIME = 1L
const val CACHE_MAX_SIZE = 10000L

class LRUCache: JsonLogicCache {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_TIME, CACHE_EXPIRY_TIME_UNIT)
        .maximumSize(CACHE_MAX_SIZE)
        .build<String, JsonLogicNode>()

    override fun containsKey(key: String): Boolean {
        return cache.getIfPresent(key) != null
    }

    override fun put(key: String, value: JsonLogicNode) {
        cache.put(key, value)
    }

    override fun get(key: String): JsonLogicNode? {
        return cache.getIfPresent(key)
    }
}
