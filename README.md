# Exzellente Informatik-Forschung (SoPra P1)

Kotlin + [BGW 0.11](https://github.com/tudo-aqua/bgw), Schichten **entity → service → view**.

```
src/main/kotlin/
├── Main.kt
├── entity/   FacultyGame, Faculty, ResearchCard, ResearchArea, ResearchTopic  (wie Klassendiagramm)
├── service/  RootService, GameService, PlayerActionService, Refreshable, AbstractRefreshingService
└── view/     SopraApplication, StartMenuScene, TurnSwitchMenuScene, GameScene, ResultMenuScene, Styles
src/test/kotlin/service/   GameServiceTest, PlayerActionServiceTest
```

Starten: `./gradlew run` (oder in IntelliJ `Main.kt`), Tests: `./gradlew test`.
Beim ersten Start lädt BGW einmalig seine Runtime (~1 Min.).

## Bedienung
1. Karte oder Stapel anklicken (gelb markiert), dann Aktion wählen.
2. Forschung vorantreiben / Strategie anpassen → Handkarte wählen
3. Publizieren → eigenen Stapel wählen · Ausbremsen → fremden Stapel wählen
4. Literatur nutzen → Karte aus der zentralen Literatur wählen
5. Bei 0 AP endet der Zug automatisch; „Zug beenden“ beendet ihn früher.

## Regelauslegungen (im Team abstimmen!)
- **Gleichstand bei Mehrheiten:** alle mit den meisten Karten +5, nächstkleinere Anzahl +2 (nur Anzahl > 0).
- **Forschungsschwerpunkt:** +4 pro Teilgebiet mit ≥ 8 publizierten Karten.
- **Strategie anpassen:** nur möglich, wenn noch ≥ 2 Karten im Nachziehstapel liegen.
- **Forschung vorantreiben bei leerem Nachziehstapel:** Karte wird gespielt, keine neue Literatur.
- **Gleichstand am Ende:** mehrere Gewinner*innen.

## Automatisierte Tests
```
./gradlew test
```
Danach: Testergebnisse in `build/reports/tests/test/index.html`,
Coverage (JaCoCo) in `build/reports/jacoco/test/html/index.html`.

| Ordner | Was wird getestet |
|---|---|
| `test/.../entity/` | Entity-Klassen (Stapel, Zugwechsel, 2–4 Spieler) |
| `test/.../service/` | Alle Aktionen, Grenzfälle, Endwertung, Spielende; `FullGameSimulationTest` spielt 30 Zufallsspiele und prüft, dass immer genau 52 Karten existieren |
| `test/.../view/` | GUI-Tests ohne Fenster: Klicks auf Buttons per `click(...)`, danach Prüfung von Labels/Karten und Spielzustand |
