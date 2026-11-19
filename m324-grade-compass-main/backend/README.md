# Grade Compass – Backend

Spring-Boot-Backend (Java 25, Spring Boot 4, Web MVC, Data JPA, Validation) von Grade Compass.
Dieses README beschreibt die wichtigsten Maven-Befehle für die Entwicklung. Eine Übersicht über das
Gesamtprojekt steht im [README im Hauptverzeichnis](../README.md).

## Voraussetzungen

- [JDK 25](https://adoptium.net/) (`java -version` prüfen)
- [Docker](https://docs.docker.com/get-docker/) mit Docker Compose (nur für die PostgreSQL-Datenbank beim normalen Start, nicht für die Tests)
- Maven muss **nicht** installiert sein: Der Maven Wrapper (`mvnw`) lädt die passende Version automatisch.

> Alle Befehle werden im Ordner `backend/` ausgeführt.
> Unter Windows `mvnw.cmd` statt `./mvnw` verwenden. Wer Maven global installiert hat, kann auch `mvn` nehmen.

## Datenbank starten (optional)

Die Anwendung braucht beim normalen Start eine PostgreSQL-Datenbank. Diese wird beim Start des Backends
(`./mvnw spring-boot:run`) automatisch über [compose.yaml](compose.yaml) gestartet (`spring-boot-docker-compose`),
sofern Docker läuft. Dieser Schritt ist daher **optional**.

Wer die Datenbank separat starten möchte, kann das mit Docker tun:

```bash
docker compose up -d
```

Das nutzt [compose.yaml](compose.yaml) und startet PostgreSQL 18 auf Port 5432
(Datenbank `gcdb`, Benutzer `gcdbUser`, Passwort `gcdbPassword123!`).

Die Verbindung lässt sich über Umgebungsvariablen anpassen: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Anwendung starten

```bash
./mvnw spring-boot:run
```

Startet das Backend (und, falls nötig, die Datenbank, siehe oben) auf <http://localhost:8080>. Test: <http://localhost:8080/api/subjects> liefert `[]`.
Die Tabellen werden beim Start automatisch erstellt (`spring.jpa.hibernate.ddl-auto=update`).

Mit einem anderen Port oder Spring-Profil:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Mit Debugger (Port 5005, z. B. aus IntelliJ per «Remote JVM Debug» verbinden):

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

## Tests ausführen

Die Tests verwenden eine In-Memory-H2-Datenbank ([src/test/resources/application.properties](src/test/resources/application.properties)).
Docker und PostgreSQL werden dafür **nicht** benötigt.

```bash
# Alle Tests
./mvnw test

# Alle Tests inkl. Integrationstests, Prüfung und Paketierung
./mvnw verify

# Nur eine Testklasse
./mvnw test -Dtest=SubjectServiceTest

# Nur eine einzelne Testmethode
./mvnw test -Dtest=SubjectServiceTest#methodenName

# Mehrere Testklassen oder ein Muster
./mvnw test -Dtest="SubjectServiceTest,GradeServiceTest"
./mvnw test -Dtest="Subject*Test"
```

Die Testberichte liegen danach in `target/surefire-reports/`.

## Bauen und als JAR ausführen

```bash
# Kompilieren (ohne Tests)
./mvnw compile

# Ausführbares JAR erstellen: target/grade-compass-0.0.1-SNAPSHOT.jar
./mvnw clean package

# Dasselbe, aber ohne Tests (schneller)
./mvnw clean package -DskipTests

# JAR starten
java -jar target/grade-compass-0.0.1-SNAPSHOT.jar

# JAR mit eigener Datenbank-Konfiguration starten
DB_URL=jdbc:postgresql://localhost:5432/gcdb DB_USER=gcdbUser DB_PASSWORD='gcdbPassword123!' \
  java -jar target/grade-compass-0.0.1-SNAPSHOT.jar
```

## Weitere nützliche Maven-Befehle

| Befehl | Erklärung |
|---|---|
| `./mvnw clean` | Löscht den Ordner `target/` (alle Build-Ergebnisse). |
| `./mvnw clean install` | Baut das Projekt neu, führt die Tests aus und installiert das Artefakt ins lokale Maven-Repository (`~/.m2`). |
| `./mvnw test-compile` | Kompiliert Code und Tests, ohne die Tests auszuführen. |
| `./mvnw dependency:tree` | Zeigt alle Abhängigkeiten (inkl. transitiver) als Baum. Hilfreich bei Versionskonflikten. |
| `./mvnw dependency:resolve` | Lädt alle Abhängigkeiten herunter (nützlich, um offline weiterzuarbeiten). |
| `./mvnw versions:display-dependency-updates` | Zeigt verfügbare neuere Versionen der Abhängigkeiten an. |
| `./mvnw help:effective-pom` | Zeigt die vollständige POM inkl. geerbter Werte vom Spring-Boot-Parent. |
| `./mvnw -v` | Zeigt die verwendete Maven- und Java-Version. |
| `./mvnw -o <ziel>` | Offline-Modus: Maven lädt nichts aus dem Internet (z. B. `./mvnw -o test`). |
| `./mvnw -q <ziel>` | Quiet-Modus: nur Fehler werden ausgegeben. |
| `./mvnw -X <ziel>` | Debug-Ausgabe von Maven, hilft bei der Fehlersuche im Build. |
| `./mvnw -U <ziel>` | Erzwingt das erneute Herunterladen von Snapshots/Abhängigkeiten. |

Wichtige Optionen:

| Option | Erklärung |
|---|---|
| `-DskipTests` | Tests werden kompiliert, aber nicht ausgeführt. |
| `-Dmaven.test.skip=true` | Tests werden weder kompiliert noch ausgeführt. |
| `-Dtest=<Name>` | Nur die angegebene Testklasse bzw. -methode ausführen. |
| `-Dspring-boot.run.profiles=<profil>` | Spring-Profil beim Start mit `spring-boot:run` aktivieren. |

## Aufräumen

```bash
# Datenbank stoppen
docker compose down

# Datenbank stoppen und alle Daten löschen
docker compose down -v
```

## Typische Probleme

- **`release version 25 not supported` / `invalid target release: 25`**: Es läuft ein älteres JDK. `java -version` und `JAVA_HOME` prüfen, JDK 25 verwenden.
- **`Connection to localhost:5432 refused`**: Die Datenbank läuft nicht. Mit `docker compose up -d` starten und mit `docker compose ps` prüfen.
- **`Port 8080 was already in use`**: Anderen Prozess beenden oder mit `--server.port=9090` einen anderen Port wählen.
- **`./mvnw: Permission denied`**: Skript ausführbar machen mit `chmod +x mvnw`.
- **Seltsame Build-Fehler nach Änderungen**: `./mvnw clean test` ausführen, um veraltete Build-Ergebnisse zu entfernen.
