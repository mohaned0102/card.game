package service

/** Basisklasse für Services, die [Refreshable]s benachrichtigen. */
abstract class AbstractRefreshingService {

    private val refreshables = mutableListOf<Refreshable>()

    /** Registriert [newRefreshable], damit es über Änderungen informiert wird. */
    fun addRefreshable(newRefreshable: Refreshable) {
        refreshables += newRefreshable
    }

    /** Führt [method] auf allen registrierten [Refreshable]s aus. */
    fun onAllRefreshables(method: Refreshable.() -> Unit) = refreshables.forEach { it.method() }
}
