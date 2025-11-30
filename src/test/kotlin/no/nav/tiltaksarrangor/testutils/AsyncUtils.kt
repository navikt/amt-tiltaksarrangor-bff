package no.nav.tiltaksarrangor.testutils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.LocalDateTime

object AsyncUtils {
	fun eventually(
		until: Duration = Duration.ofSeconds(3),
		interval: Duration = Duration.ofMillis(100),
		func: () -> Unit,
	) = no.nav.tiltaksarrangor.testutils
		.eventually(until, interval, func)
}

fun eventually(
	until: Duration = Duration.ofSeconds(3),
	interval: Duration = Duration.ofMillis(100),
	func: () -> Unit,
) = runBlocking {
	val untilTime = LocalDateTime.now().plusNanos(until.toNanos())

	var throwable: Throwable = kotlin.IllegalStateException()

	while (LocalDateTime.now().isBefore(untilTime)) {
		try {
			func()
			return@runBlocking
		} catch (t: Throwable) {
			throwable = t
			delay(interval)
		}
	}

	throw kotlin.AssertionError(throwable)
}
