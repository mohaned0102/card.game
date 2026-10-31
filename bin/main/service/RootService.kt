package service

import entity.FacultyGame

/** Hält das aktuelle Spiel und verbindet alle Services miteinander. */
class RootService {

    val gameService = GameService(this)
    val playerActionService = PlayerActionService(this)

    /** Das laufende Spiel oder `null`, falls noch keins gestartet wurde. */
    var currentGame: FacultyGame? = null

    /** Registriert [newRefreshable] bei allen Services. */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }

    /** Registriert alle [newRefreshables] bei allen Services. */
    fun addRefreshables(vararg newRefreshables: Refreshable) = newRefreshables.forEach(::addRefreshable)

    /**
     * Schreibt eine menschenlesbare Zeile in den Log (Konsole) und informiert die View.
     */
    fun log(message: String) {
        println(message)
        gameService.onAllRefreshables { refreshAfterLog(message) }
    }
}
