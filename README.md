# Exzellente Informatik-Forschung

Kartenspiel für 2–4 Spieler im Hotseat-Modus, entstanden im Softwarepraktikum (SoPra) an der TU Dortmund.
Umgesetzt in Kotlin mit dem GUI-Framework [BGW](https://github.com/tudo-aqua/bgw).

Autor: _Name_ · Rolle im Team: _z.B. Spiellogik und Tests_

Der größte Teil der Arbeit steckt neben der Spiellogik in der Testabdeckung. Dieses README beschreibt
deshalb vor allem, wie das Projekt getestet wird und warum die Tests so aufgebaut sind.

## Starten

Benötigt wird ein JDK ab Version 17 (getestet mit 21 und 25). Gradle lädt der Wrapper selbst.

```
./gradlew run          # Spiel starten
./gradlew test         # alle automatisierten Tests
./gradlew visualTest   # sichtbarer Test im echten Fenster
```

Beim ersten Start lädt BGW seine Runtime, das dauert etwa eine Minute. Unter Windows sollte das Projekt
nicht in einem OneDrive-Ordner liegen, sonst bricht der Build mit `Cannot snapshot ... not a regular file` ab.

## Aufbau

Das Projekt ist in drei Schichten getrennt:

- `entity/` – nur der Spielzustand (Spieler, Karten, Stapel), keine Logik
- `service/` – Spielregeln: `GameService` (Start, Zugwechsel, Spielende, Wertung) und
  `PlayerActionService` (die sechs Aktionen)
- `view/` – BGW-Oberfläche; die Services benachrichtigen sie über das Interface `Refreshable`

Diese Trennung macht das Testen einfach: Die Regeln lassen sich komplett ohne Oberfläche prüfen,
und die Oberfläche lässt sich mit einem echten `RootService` testen, ohne ein Fenster zu öffnen.

## Teststrategie

Insgesamt gibt es 62 automatisierte Tests und einen sichtbaren End-to-End-Test.
Line Coverage laut JaCoCo: Service 97 %, Entity 96 %, View 92 %.

| Ebene | Klassen | Tests | Was geprüft wird |
|---|---|---:|---|
| Unit (Entity) | `EntityTest` | 5 | Stapel, Zugwechsel, 2–4 Spieler |
| Unit (Service) | `GameServiceTest`, `PlayerActionServiceTest` | 15 | Spielaufbau, alle Aktionen, Endwertung |
| Grenzfälle | `RuleEdgeCaseTest` | 16 | ungültige Züge, leere Stapel, Gleichstand |
| Simulation | `FullGameSimulationTest` | 1 | 30 zufällige Partien mit Invarianten |
| GUI | `StartMenuSceneTest`, `GameSceneTest`, `ResultMenuSceneTest` | 25 | Klicks, Anzeige, Fehlermeldungen |
| End-to-End | `VisualGameTest` | – | komplette Partie im echten Fenster |

### Unit-Tests und Grenzfälle

Jeder Test folgt dem Muster Arrange – Act – Assert und bekommt über `@BeforeTest` ein neues Spiel, damit
kein Test vom anderen abhängt. Zu jeder Aktion gibt es neben dem Normalfall auch Negativtests, z.B.:

- eine siebte Handkarte wird abgelehnt
- Publizieren mit nur einer Karte ist nicht erlaubt
- eine 2-AP-Aktion mit nur 1 AP schlägt fehl
- nach Spielende sind keine Aktionen mehr möglich

Ungültige Züge werfen im Service eine Exception mit lesbarer Meldung. Die Tests prüfen das mit
`assertFailsWith`, die Oberfläche zeigt die Meldung an, statt abzustürzen.

Ob die Services die Oberfläche richtig benachrichtigen, wird mit einem Test-Double geprüft:
`TestRefreshable` zählt mit, welche `refreshAfter...()`-Methoden aufgerufen wurden, und sammelt die
Log-Zeilen.

### Simulation mit Invarianten

`FullGameSimulationTest` lässt zufällige Bots 30 komplette Partien mit 2, 3 und 4 Spielern spielen.
Nach jedem einzelnen Zug wird geprüft:

- es gibt immer genau 52 Karten, keine geht verloren, keine ist doppelt
- niemand hat mehr als 6 Handkarten, in der Literatur liegen höchstens 6 Karten
- jede Partie endet, und zwar erst, wenn der Nachziehstapel leer ist

Die Idee dahinter: Einzelne Tests decken nur Situationen ab, an die man selbst gedacht hat. Die Simulation
erzeugt tausende Spielstände, die man nie von Hand konstruieren würde. Die Seeds sind fest, damit ein
Fehler reproduzierbar bleibt.

### GUI-Tests ohne Fenster

BGW-Komponenten sind normale Kotlin-Objekte, der Klick-Handler steckt in `onMouseClicked`. Die
Hilfsfunktion `click(...)` ruft ihn direkt auf. Danach werden Anzeige und Spielzustand geprüft:

```kotlin
click(scene.handButtons[0])     // Karte auswählen
click(scene.advanceButton)      // "Forschung vorantreiben"
assertEquals(4, scene.handButtons.count { it.isVisible })
```

Damit die Tests an die Komponenten kommen, sind diese in den Scenes `internal` statt `private`.
Getestet wird unter anderem, dass die Hand beim Spielerwechsel verdeckt ist, dass Fehlermeldungen
erscheinen und dass das Startmenü ungültige Eingaben (ein Spieler, doppelte Namen) abfängt.

### Sichtbarer End-to-End-Test

```
./gradlew visualTest
./gradlew visualTest -Pdelay=1000   # langsamer
```

Öffnet das echte Fenster und spielt eine Partie mit drei Spielern durch, so wie ein Mensch: Namen
eintragen, Reihenfolge ändern, starten, in jedem Zug Karten anklicken und Aktionen wählen, bis zur
Ergebnisanzeige. Nach jedem Schritt laufen dieselben Invarianten wie in der Simulation, außerdem wird
geprüft, dass die Ergebnisanzeige den richtigen Gewinner nennt. Bei einem Fehler endet der Task mit
Exit-Code 1.

Das ist bewusst ein eigener Gradle-Task und kein JUnit-Test: BGW beendet beim Schließen des Fensters
die ganze JVM, außerdem braucht der Test einen Bildschirm und passt deshalb nicht in eine CI-Pipeline.

## Tests ausführen

```
./gradlew test                                          # alle
./gradlew test --tests "service.PlayerActionServiceTest" # eine Klasse
./gradlew test --tests "*publish*"                      # alle Tests zu einer Funktion
./gradlew test --rerun                                  # erneut, auch ohne Änderungen
```

In IntelliJ geht das auch über den grünen Pfeil neben jedem Test.

Berichte nach dem Lauf:

- Testergebnisse: `build/reports/tests/test/index.html`
- Coverage: `build/reports/jacoco/test/html/index.html`

## Gefundene Fehler

Ein paar Beispiele, die erst durch die Tests aufgefallen sind:

- Ein Test war selbst falsch: zweimal nachziehen bei 5 Handkarten verletzt das Limit von 6. Der Fehler
  lag im Test, nicht im Spiel.
- BGW-Labels stellen `\n` nicht als Zeilenumbruch dar. Der Log im Spielfeld lief dadurch in eine Zeile,
  jetzt hat jede Zeile ein eigenes Label.
- Im sichtbaren Test hat ein zweiter Klick auf eine schon gewählte Karte die Auswahl wieder aufgehoben,
  wodurch Aktionen scheinbar grundlos fehlschlugen.

## Offene Regelfragen

Diese Punkte waren in der Aufgabe nicht eindeutig. Jede Entscheidung ist durch einen Test abgesichert:

- Gleichstand bei Mehrheiten: alle mit den meisten Karten +5, die nächstkleinere Anzahl +2
- Forschungsschwerpunkt: +4 für jedes Teilgebiet mit mindestens 8 publizierten Karten
- „Strategie anpassen“ nur, wenn noch 2 Karten im Nachziehstapel liegen
- Gleichstand am Ende: mehrere Gewinner

## Ausblick

- Tests automatisch bei jedem Push in einer GitLab-CI-Pipeline laufen lassen
- Mutation Testing (z.B. PIT), um zu prüfen, ob die Tests Fehler im Code wirklich finden und nicht nur
  Zeilen ausführen


