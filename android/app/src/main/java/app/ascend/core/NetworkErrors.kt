package app.ascend.core

import java.io.IOException
import java.net.UnknownHostException

/**
 * True when [t] is a connectivity failure (no network / DNS / socket) rather
 * than an HTTP or parsing error — used to show an "offline" message + retry
 * instead of a generic error.
 */
fun Throwable.isOffline(): Boolean = when (this) {
    is UnknownHostException -> true
    is IOException -> true   // SocketTimeout, ConnectException, etc. (HttpException is not an IOException)
    else -> cause?.let { it !== this && it.isOffline() } ?: false
}

/** User-facing message for a caught throwable, distinguishing offline from generic. */
fun Throwable.userMessage(generic: String): String =
    if (isOffline()) "You're offline. Check your connection and try again." else (message ?: generic)
