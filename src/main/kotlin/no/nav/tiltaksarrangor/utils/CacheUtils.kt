package no.nav.tiltaksarrangor.utils

import com.github.benmanes.caffeine.cache.Cache

object CacheUtils {
	fun <K : Any, V : Any> tryCacheFirstNullable(
		cache: Cache<K, V>,
		key: K,
		valueSupplier: () -> V?,
	): V? {
		val value = cache.getIfPresent(key)

		if (value == null) {
			val newValue: V = valueSupplier.invoke() ?: return null
			cache.put(key, newValue)
			return newValue
		}

		return value
	}

	fun <K : Any, V : Any> tryCacheFirstNotNull(
		cache: Cache<K, V>,
		key: K,
		valueSupplier: () -> V,
	): V = tryCacheFirstNullable(cache, key) { valueSupplier.invoke() }!!
}
