# DiceApp

This app is meant to be used as an alternative to real Dice when playing Games like D&D.

## Strukturvorschlag

1. **Core-Module**
    - Benutzerprofilmanagement
    - Würfelmechanik
    - Session-Handling

2. **Feature-Module**
    - Skill-Check-System
    - Chat und Historie
    - Gamemaster-Tools

## Zusätzliche Features

1. **Charakter-Sheets**
    - Integrierte Charakterbögen für verschiedene Systeme (D&D, Das Schwarze Auge, etc.)
    - Automatische Berechnung von Modifikatoren

2. **Würfel-Presets**
    - Gespeicherte Würfelkombinationen für wiederkehrende Checks
    - "Favoriten" für schnellen Zugriff

3. **Statistik-Tracking**
    - Würfelstatistiken über längere Zeit
    - Heatmaps für "heiße" und "kalte" Würfel

4. ~~**Offline-Modus**~~
    - Lokales Speichern von Sessions für Spiel ohne Internetverbindung
    - Synchronisierung bei Wiederverbindung

5. **Audio-Feedback**
    - Verschiedene Würfelgeräusche je nach Ergebnis
    - Besondere Effekte bei kritischen Erfolgen/Fehlschlägen

6. **Visuelles Feedback**
    - Animation des Würfelwurfs
    - Thematische Hintergründe je nach Spielsystem

7. ~~**Multi-Plattform-Support**~~
    - Web, Desktop und Mobile aus einer Codebasis
    - Synchronisierung zwischen Geräten

8. **Initiative-Tracker**
    - Visualisierung der Kampfreihenfolge
    - Automatische Sortierung

9. **Regelwerk-Integration**
    - Kurze Regelreferenzen direkt in der App
    - Kontextbezogene Hilfe

10. ~~**Sprachsteuerung**~~
    - Würfeln per Sprachbefehl ("Würfle Angriff mit Schwert")

## Technologieempfehlungen für Kotlin

- **Jetpack Compose** für modernes UI
- **Kotlin Coroutines** für asynchrone Operationen
- **Kotlin Multiplatform** falls du verschiedene Plattformen unterstützen willst
- **Room/SQLDelight** für lokale Datenhaltung
- **Ktor** für Netzwerkkommunikation
- **Koin/Dagger-Hilt** für Dependency Injection