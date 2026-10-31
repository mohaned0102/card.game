# Exzellente Informatik-Forschung – SoPra P1

Taktisches Kartenspiel für 2–4 Personen im Hotseat-Modus, umgesetzt in **Kotlin** mit dem GUI-Framework
[BGW 0.11](https://github.com/tudo-aqua/bgw).

Der Schwerpunkt dieses READMEs liegt auf den **automatisierten Tests**: was getestet wird, wie man die Tests
ausführt und wie man neue Tests schreibt.

---

## Inhalt

1. [Schnellstart](#1-schnellstart)
2. [Projektstruktur](#2-projektstruktur)
3. [Teststrategie im Überblick](#3-teststrategie-im-überblick)
4. [Tests ausführen](#4-tests-ausführen)
5. [Einzelne Tests ausführen](#5-einzelne-tests-ausführen)
6. [Was genau getestet wird](#6-was-genau-getestet-wird)
7. [Sichtbarer GUI-Test (`visualTest`)](#7-sichtbarer-gui-test-visualtest)
8. [Testberichte und Coverage](#8-testberichte-und-coverage)
9. [Eigene Tests schreiben](#9-eigene-tests-schreiben)
10. [Bedienung des Spiels](#10-bedienung-des-spiels)
11. [Regelauslegungen](#11-regelauslegungen)
12. [Fehlerbehebung](#12-fehlerbehebung)

---

## 1. Schnellstart

**Voraussetzung:** JDK 17 oder neuer (getestet mit Java 21 und Java 25). Gradle muss nicht installiert sein,
der Gradle Wrapper (`gradlew`) lädt Gradle 9.2.1 automatisch.

| Ziel | Befehl |
|---|---|
| Spiel starten | `./gradlew run` |
| Alle automatisierten Tests | `./gradlew test` |
| Sichtbarer GUI-Test | `./gradlew visualTest` |

Beim ersten Start lädt BGW einmalig seine Runtime herunter (ca. 1 Minute).

> **Windows:** Das Projekt **nicht** in einem OneDrive-Ordner ablegen (z.B. `C:\dev\sopra-p1` verwenden),
> siehe [Fehlerbehebung](#12-fehlerbehebung).

---

## 2. Projektstruktur

Das Projekt folgt der SoPra-Schichtenarchitektur **entity → service → view**.

```
src/
├── main/kotlin/
│   ├── Main.kt
│   ├── entity/    FacultyGame, Faculty, ResearchCard, ResearchArea, ResearchTopic
│   ├── service/   RootService, GameService, PlayerActionService,
│   │              Refreshable, AbstractRefreshingService
│   └── view/      SopraApplication, StartMenuScene, GameScene,
│                  TurnSwitchMenuScene, ResultMenuScene, Styles
└── test/kotlin/
    ├── entity/    EntityTest
    ├── service/   GameServiceTest, PlayerActionServiceTest, RuleEdgeCaseTest,
    │              FullGameSimulationTest, TestRefreshable
    └── view/      StartMenuSceneTest, GameSceneTest, ResultMenuSceneTest,
                   GuiTestUtil, VisualGameTest
```

| Schicht | Aufgabe |
|---|---|
| **entity** | Reiner Spielzustand, entspricht dem vorgegebenen Klassendiagramm |
| **service** | Spiellogik: Regeln prüfen, Aktionen ausführen, Endwertung, Konsolen-Log |
| **view** | BGW-Oberfläche; wird von den Services über `Refreshable` benachrichtigt |

---

## 3. Teststrategie im Überblick

Es gibt **62 automatisierte Tests** plus einen **sichtbaren End-to-End-Test**.

```
                 ┌────────────────────────────┐
                 │  visualTest (1 Partie)     │  echtes Fenster, sichtbar, manuell gestartet
                 ├────────────────────────────┤
                 │  GUI-Tests (25)            │  Klicks auf Komponenten, ohne Fenster
                 ├────────────────────────────┤
                 │  Simulation (30 Partien)   │  Zufalls-Bots, Invarianten
                 ├────────────────────────────┤
                 │  Service-Tests (31)        │  Regeln, Grenzfälle, Wertung
                 ├────────────────────────────┤
                 │  Entity-Tests (5)          │  Datenmodell
                 └────────────────────────────┘
```

| Ebene | Testklassen | Anzahl | Fenster nötig? | Läuft mit |
|---|---|---:|:---:|---|
| Entity | `EntityTest` | 5 | nein | `./gradlew test` |
| Service | `GameServiceTest`, `PlayerActionServiceTest`, `RuleEdgeCaseTest` | 31 | nein | `./gradlew test` |
| Simulation | `FullGameSimulationTest` | 1 (30 Partien) | nein | `./gradlew test` |
| GUI | `StartMenuSceneTest`, `GameSceneTest`, `ResultMenuSceneTest` | 25 | nein | `./gradlew test` |
| End-to-End | `VisualGameTest` | 1 Partie | **ja** | `./gradlew visualTest` |

**Werkzeuge:** `kotlin.test` auf JUnit 5, JaCoCo 0.8.14 für die Coverage.

**Coverage (Zeilen):** service 97 % · entity 96 % · view 92 %.
Nicht abgedeckt sind nur `Main.kt` und `SopraApplication`, weil diese ein echtes Fenster öffnen.
Diese beiden werden durch den `visualTest` geprüft.

---

## 4. Tests ausführen

### Alle automatisierten Tests

```bash
./gradlew test
```

- Läuft komplett **ohne Fenster**, also auch in einer CI-Pipeline.
- Danach wird automatisch der Coverage-Bericht erzeugt (siehe [Abschnitt 8](#8-testberichte-und-coverage)).
- Ergebnis: `BUILD SUCCESSFUL` = alle Tests bestanden, `BUILD FAILED` = mindestens ein Test fehlgeschlagen.

### Tests neu ausführen, obwohl sich nichts geändert hat

Gradle überspringt Tests, wenn sich seit dem letzten Lauf nichts geändert hat. So erzwingt man einen neuen Lauf:

```bash
./gradlew test --rerun
```

### In IntelliJ

- Rechtsklick auf `src/test/kotlin` → **Run 'All Tests'**
- Oder im Gradle-Fenster: `Tasks → verification → test`

---

## 5. Einzelne Tests ausführen

Jeder Test ist unabhängig: `@BeforeTest` erzeugt vor jedem Test ein frisches Spiel.

### In IntelliJ

Den grünen ▶-Pfeil neben einer Testmethode anklicken, um nur diesen Test auszuführen,
oder neben der Klasse, um alle Tests dieser Klasse auszuführen.

### Mit Gradle (`--tests`)

Einen einzelnen Test:
```bash
./gradlew test --tests "service.PlayerActionServiceTest.hand limit is six"
```

Eine ganze Testklasse:
```bash
./gradlew test --tests "service.PlayerActionServiceTest"
```

Eine ganze Schicht:
```bash
./gradlew test --tests "view.*"
```

Alle Tests zu einer Funktion (Platzhalter `*`):
```bash
./gradlew test --tests "*publish*"
```

> Die Namensfilter unterscheiden Groß- und Kleinschreibung: `"*publish*"` findet Tests, `"*Publish*"` nicht.

### Tests pro Spielfunktion

| Funktion | Befehl |
|---|---|
| Forschung vorantreiben | `./gradlew test --tests "*advance research*"` |
| Neue Ideen gewinnen | `./gradlew test --tests "*new idea*" --tests "*hand limit*"` |
| Strategie anpassen | `./gradlew test --tests "*strategy*"` |
| Publizieren | `./gradlew test --tests "*publish*" --tests "*journal*"` |
| Konkurrenz ausbremsen | `./gradlew test --tests "*slow down*"` |
| Literatur nutzen | `./gradlew test --tests "*literature*"` |
| Spielstart / Konfiguration | `./gradlew test --tests "service.GameServiceTest" --tests "view.StartMenuSceneTest"` |
| Spielende | `./gradlew test --tests "*game ends*" --tests "*more turns*"` |
| Endwertung | `./gradlew test --tests "*scoring*" --tests "*majority*" --tests "*winners*"` |
| Nur Backend | `./gradlew test --tests "service.*" --tests "entity.*"` |
| Nur GUI | `./gradlew test --tests "view.*"` |

---

## 6. Was genau getestet wird

### 6.1 Entity – `EntityTest` (5)

| Test | Prüft |
|---|---|
| faculty starts empty and resets to two action points | Leere Stapel zu Beginn, `resetActionPoints()` setzt 2 AP |
| addResearch puts card on the stack of its suit | Karte landet oben auf dem Stapel ihres Teilgebiets |
| nextFaculty wraps around clockwise | Zugwechsel im Uhrzeigersinn, nach der letzten Fakultät wieder die erste |
| game needs two to four faculties | 1 oder 5 Fakultäten werden abgelehnt |
| cards are displayed with suit symbol and value | Lesbare Darstellung, z.B. `♥Q` |

### 6.2 Spielablauf – `GameServiceTest` (5)

| Test | Prüft |
|---|---|
| start deals 5 cards each and 6 literature cards | Spielaufbau: 5 Handkarten pro Fakultät, 6 Karten Literatur, Rest im Nachziehstapel |
| invalid player configurations are rejected | Zu wenige/viele Spieler, doppelte oder leere Namen |
| random order keeps all names | Zufällige Reihenfolge verliert keine Spieler*innen |
| game ends after every other faculty had one more turn | Spielende: Nachziehstapel leer → jede andere Fakultät noch einmal am Zug |
| final scoring applies all bonuses | Sichtbarkeit (+5/+2), Breite (+3), Schwerpunkt (+4), angefangene Forschung (+1/Karte) |

### 6.3 Aktionen – `PlayerActionServiceTest` (10)

| Test | Prüft |
|---|---|
| advance research moves card and refreshes literature | Karte auf eigenen Stapel, neue Literatur, älteste Karte fliegt raus |
| turn ends automatically when action points are used up | Bei 0 AP wechselt der Zug automatisch |
| hand limit is six | Maximal 6 Handkarten |
| adjust strategy discards one, draws two and keeps one | Ablauf von „Strategie anpassen“ |
| publish scores points and first publication bonus | Punkte + Bonus für die erste Publikation im Teilgebiet (nur einmal) |
| publishing needs at least two cards | Publizieren erst ab 2 Karten |
| slow down removes top card of another faculty | Oberste fremde Karte kommt auf den Ablagestapel |
| cannot slow down yourself | Eigene Stapel können nicht ausgebremst werden |
| use literature takes any card and closes the gap | Beliebige Karte nehmen, Lücke schließt sich |
| publication points table | 2 → 3, 3 → 5, 4 → 7, 5+ → 9 Punkte |

### 6.4 Grenzfälle – `RuleEdgeCaseTest` (16)

| Test | Prüft |
|---|---|
| start notifies view and writes log | Spielstart informiert die View und schreibt in den Log |
| every action notifies the view and is logged | Jede Aktion wird gemeldet und menschenlesbar geloggt |
| advance research with empty draw stack adds no literature | Leerer Nachziehstapel: keine neue Literatur |
| advance research rejects card not on hand | Fremde Karte wird abgelehnt, keine AP verbraucht |
| new idea fails on empty draw stack | Nachziehen bei leerem Stapel unmöglich |
| two point actions need two action points | 2-AP-Aktion mit nur 1 AP wird abgelehnt |
| strategy needs two cards in draw stack | „Strategie anpassen“ braucht 2 Karten im Nachziehstapel |
| strategy choice blocks other actions and end turn | Solange gewählt werden muss, sind andere Aktionen gesperrt |
| keeping without pending strategy fails | Kartenwahl ohne vorherige Strategie-Aktion wird abgelehnt |
| strategy as last action ends the turn after choosing | Zugwechsel erst nach der Kartenwahl |
| journal article for five or more cards | 5+ Karten → Journal-Artikel (9 Punkte) |
| slow down on empty stack fails | Ausbremsen eines leeren Stapels unmöglich |
| actions are rejected after game end | Nach Spielende keine Aktionen mehr |
| four players get three more turns after draw stack is empty | Endphase mit 4 Spieler*innen |
| majority tie gives everyone with most cards five points | Gleichstand bei Mehrheiten |
| equal totals produce several winners | Gleichstand bei der Gesamtpunktzahl |

### 6.5 Simulation – `FullGameSimulationTest` (1 Test, 30 Partien)

Zufällige „Bots“ spielen 30 komplette Partien mit 2, 3 und 4 Spieler*innen (feste Seeds, also
reproduzierbar). Nach **jedem einzelnen Schritt** wird geprüft:

- Es gibt **immer genau 52 Karten**, keine geht verloren, keine existiert doppelt.
- Höchstens 6 Handkarten pro Fakultät, höchstens 6 Karten in der Literatur.
- Jede Partie endet tatsächlich (kein Endlos-Spiel) und erst, wenn der Nachziehstapel leer ist.

Dieser Test findet Fehler, an die man beim Schreiben einzelner Tests nicht denkt.

### 6.6 GUI-Tests ohne Fenster (25)

**`StartMenuSceneTest` (6)**

| Test | Prüft |
|---|---|
| start button creates game in entered order | Namen aus den Feldern, leere Felder werden übersprungen |
| move up button swaps order | ▲-Button ändert die Reihenfolge |
| shuffle keeps all names and moves empty fields to the end | „Reihenfolge mischen“ |
| too few players shows error and starts no game | Fehlermeldung statt Absturz |
| duplicate names show error | Doppelte Namen werden abgelehnt |
| random order checkbox still uses all names | Checkbox „Beim Start zufällig mischen“ |

**`GameSceneTest` (16)**

| Test | Prüft |
|---|---|
| hand is hidden at turn start and shown after reveal | Hotseat: Hand verdeckt bis „Bereit“ |
| status shows current faculty and action points | Statuszeile |
| literature shows six cards | Anzeige der zentralen Literatur |
| selecting a hand card highlights it and toggles off again | Auswahl und Abwahl |
| advance research via clicks | Karte wählen → Aktion → Anzeige aktualisiert |
| action without selection shows error message | Hinweis „Wähle zuerst …“ |
| rule violation is shown instead of crashing | Regelverstoß wird als Meldung angezeigt |
| publish own stack via clicks | Publizieren per Klick, Punkte und Zugwechsel |
| cannot publish stack of another faculty | Fremden Stapel publizieren wird abgelehnt |
| slow down competition via clicks | Ausbremsen per Klick |
| use literature via clicks | Literatur nutzen per Klick |
| strategy shows two cards to choose from | Auswahl-Bereich bei „Strategie anpassen“ |
| end turn button passes turn to next faculty | „Zug beenden“ |
| stack buttons show cards in order | Reihenfolge der Karten auf dem Stapel sichtbar |
| only panels of existing faculties are visible | Bei 2 Spieler*innen nur 2 Bereiche |
| log shows latest actions | Log-Anzeige im Spielfeld |

**`ResultMenuSceneTest` (3)**

| Test | Prüft |
|---|---|
| shows single winner and all rows | Gewinner*in und Punkteliste |
| shows tie | Anzeige bei Gleichstand |
| turn switch names next faculty | Text des Hotseat-Zwischenbildschirms |

**Wie GUI-Tests ohne Fenster funktionieren:** Eine BGW-Komponente ist ein normales Kotlin-Objekt, ihr
Klick-Handler steckt in `onMouseClicked`. Die Hilfsfunktion `click(...)` in `GuiTestUtil.kt` ruft diesen
Handler direkt auf, so als hätte jemand geklickt. Danach werden Labels, Karten und der Spielzustand geprüft:

```kotlin
click(scene.handButtons[0])       // Karte auswählen
click(scene.advanceButton)        // "Forschung vorantreiben"
assertEquals(4, scene.handButtons.count { it.isVisible })
```

Damit die Tests auf die Komponenten zugreifen können, sind diese in den Scenes `internal` statt `private`
(sichtbar im selben Modul, also auch für die Tests, aber nicht von außen).

---

## 7. Sichtbarer GUI-Test (`visualTest`)

```bash
./gradlew visualTest
```

Langsamer zuschauen (Pause zwischen zwei Klicks in Millisekunden, Standard: 400):
```bash
./gradlew visualTest -Pdelay=1000
```

**Ablauf:** Das echte Spielfenster öffnet sich und eine komplette Partie läuft automatisch ab:

1. Drei Namen eintragen (Dortmund, Bochum, Essen) und die Reihenfolge mit ▲ ändern
2. „Spiel starten“
3. Jeder Zug: „Bereit“ klicken, dann Karte oder Stapel auswählen und eine Aktion klicken (alle 6 Aktionen kommen vor)
4. Bis zum Spielende und zur Ergebnisanzeige

**Prüfungen nach jedem Schritt:**

- Immer genau 52 Karten im Spiel
- Handkartenlimit (6) und Literatur (≤ 6) eingehalten
- Hand beim Spielerwechsel verdeckt
- Statuszeile zeigt die richtige Fakultät
- Abgelehnte Aktionen zeigen eine Fehlermeldung
- Ergebnisanzeige nennt die richtige Gewinnerin bzw. den richtigen Gewinner

**Ausgabe in der Konsole:**
```
✔ [12] Forschung vorantreiben (Essen)
✔ [13] Publizieren (Bochum)
...
✅ VISUELLER TEST BESTANDEN (44 Schritte)
```

Bei einem Fehler erscheint `❌` mit Grund. Das Fenster bleibt dann 6 Sekunden offen, und Gradle meldet
`BUILD FAILED` (Exit-Code 1).

**Warum ein eigener Task und nicht Teil von `./gradlew test`?**

- Beim Schließen des Fensters beendet BGW die komplette JVM (`exitProcess`). Innerhalb von JUnit würde das
  den Testlauf abbrechen.
- Der Test braucht einen Bildschirm und eignet sich daher nicht für CI-Pipelines.

Er ist gedacht zum Zuschauen, für Demos (z.B. beim Projektabschluss) und als letzter Check vor der Abgabe.

---

## 8. Testberichte und Coverage

Nach `./gradlew test` werden zwei HTML-Berichte erzeugt.

| Bericht | Pfad | Inhalt |
|---|---|---|
| Testergebnisse | `build/reports/tests/test/index.html` | Jeder Test mit Status und Dauer; bei Fehlern erwarteter und tatsächlicher Wert |
| Coverage (JaCoCo) | `build/reports/jacoco/test/html/index.html` | Abdeckung pro Paket, Klasse und Zeile |

Unter Windows öffnen:
```bash
start build\reports\tests\test\index.html
```
```bash
start build\reports\jacoco\test\html\index.html
```

**Farben im Coverage-Bericht:** grün = ausgeführt, gelb = Verzweigung nur teilweise getestet
(z.B. nur der `if`-Zweig), rot = nie ausgeführt.

Zusätzlich gibt es einen XML-Bericht (`build/reports/jacoco/test/jacocoTestReport.xml`), den z.B. GitLab CI
auswerten kann.

---

## 9. Eigene Tests schreiben

### Aufbau: Arrange → Act → Assert

```kotlin
@Test
fun `hand limit is six`() {
    // Arrange: Ausgangszustand herstellen (Spiel kommt aus @BeforeTest)
    val faculty = game.currentFaculty

    // Act: Aktion ausführen
    actions.gainNewIdea()

    // Assert: Ergebnis prüfen
    assertEquals(6, faculty.handCards.size)
    assertFailsWith<IllegalStateException> { actions.gainNewIdea() }
}
```

### Nützliche Bausteine

| Baustein | Wo | Wofür |
|---|---|---|
| `@BeforeTest` | jede Testklasse | Frisches Spiel vor jedem Test, damit Tests unabhängig bleiben |
| `TestRefreshable` | `test/.../service/` | Test-Double für die View: zählt `refreshAfter…()`-Aufrufe und sammelt Log-Zeilen |
| `click(component)` | `test/.../view/GuiTestUtil.kt` | Simuliert einen Mausklick ohne Fenster |
| `startNewGame(..., deck = …)` | `GameService` | Festes Deck statt Zufall, für reproduzierbare Tests |
| Zustand direkt setzen | z.B. `faculty.addResearch(card)` | Gezielt eine Spielsituation herstellen |

### Beispiel: Prüfen, ob die View benachrichtigt wird

```kotlin
val view = TestRefreshable()
root.addRefreshable(view)
actions.gainNewIdea()
assertEquals(1, view.actionCalls)
```

### Regeln für gute Tests

- **Ein Test, eine Aussage:** Der Testname beschreibt das erwartete Verhalten.
- **Auch das Verbotene testen:** Zu jeder Aktion gehört mindestens ein Test, der prüft, dass ungültige Züge
  abgelehnt werden (`assertFailsWith`).
- **Keine Abhängigkeiten zwischen Tests:** Kein Test darf sich auf das Ergebnis eines anderen verlassen.
- **Kein Zufall ohne Seed:** Zufall nur mit festem Seed (`Random(42)`), sonst sind Fehler nicht reproduzierbar.
- **Neuer Fehler gefunden?** Zuerst einen Test schreiben, der ihn zeigt (rot), dann den Fehler beheben (grün).

---

## 10. Bedienung des Spiels

1. **Startmenü:** 2–4 Namen eintragen. Die Reihenfolge der Felder ist die Spielreihenfolge. Sie lässt sich mit
   ▲ ändern, mit „Reihenfolge mischen“ zufällig festlegen oder über die Checkbox beim Start mischen.
2. **Spielerwechsel:** Ein Zwischenbildschirm verdeckt die Hand, bis die nächste Person „Bereit“ klickt
   (Hotseat).
3. **Aktionen:** Erst Karte oder Stapel anklicken (wird gelb markiert), dann die Aktion wählen.

| Aktion | AP | Auswahl vorher |
|---|---:|---|
| Forschung vorantreiben | 1 | Handkarte |
| Neue Ideen gewinnen | 1 | – |
| Strategie anpassen | 1 | Handkarte zum Abwerfen, danach eine der 2 gezogenen Karten wählen |
| Publizieren | 2 | Eigener Stapel (≥ 2 Karten) |
| Konkurrenz ausbremsen | 2 | Stapel einer anderen Fakultät |
| Literatur nutzen | 2 | Karte aus der zentralen Literatur |

4. Bei 0 AP endet der Zug automatisch; „Zug beenden“ beendet ihn früher.
5. **Log:** Alle Aktionen erscheinen menschenlesbar in der Konsole, die letzten 5 auch im Spielfeld.
6. **Spielende:** Die Ergebnisanzeige zeigt die Punkte jeder Fakultät aufgeschlüsselt und wer gewonnen hat.

---

## 11. Regelauslegungen

Diese Punkte sind in der Aufgabenstellung nicht eindeutig und sind jeweils durch Tests abgesichert.

| Situation | Umsetzung | Test |
|---|---|---|
| Gleichstand bei Mehrheiten | Alle mit den meisten Karten +5, nächstkleinere Anzahl +2 (nur Anzahl > 0) | `majority tie gives everyone with most cards five points` |
| Forschungsschwerpunkt | +4 pro Teilgebiet mit ≥ 8 publizierten Karten | `final scoring applies all bonuses` |
| Strategie anpassen | Nur möglich, wenn noch ≥ 2 Karten im Nachziehstapel liegen | `strategy needs two cards in draw stack` |
| Forschung vorantreiben bei leerem Nachziehstapel | Karte wird gespielt, keine neue Literatur | `advance research with empty draw stack adds no literature` |
| Gleichstand am Spielende | Mehrere Gewinner*innen | `equal totals produce several winners` |

---

## 12. Fehlerbehebung

| Fehlermeldung | Ursache | Lösung |
|---|---|---|
| `What went wrong: 25.0.4.1` (o.ä. Versionsnummer) | Gradle-Version zu alt für das installierte JDK | Wrapper nutzt Gradle 9.2.1; mit `./gradlew` statt `gradle` starten |
| `Cannot snapshot …: not a regular file` | Projekt liegt in OneDrive (Dateien nur in der Cloud) | Projekt nach `C:\dev\…` verschieben |
| `… Daemons could not be reused` | Alte Gradle-Prozesse | `./gradlew --stop`, dann erneut starten |
| Tests laufen nicht erneut | Gradle hält sie für aktuell | `./gradlew test --rerun` |
| `visualTest` öffnet kein Fenster | BGW-Runtime wird beim ersten Start geladen | ca. 1 Minute warten |

