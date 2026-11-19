# Grade Compass

Grade Compass ist eine kleine Web-Anwendung zur Verwaltung von Schulfächern und Noten (Modul 324, BBZW).
Aktuell können Fächer angelegt, angezeigt, bearbeitet und gelöscht werden. Noten sind im Backend in Arbeit.

## Architektur

Die Anwendung besteht aus drei Teilen: einem Angular-Frontend, einem Spring-Boot-Backend und einer PostgreSQL-Datenbank.

```
┌──────────────────┐   HTTP / JSON    ┌──────────────────┐   JDBC    ┌──────────────┐
│     Frontend     │ ───────────────▶│     Backend      │ ────────▶│  PostgreSQL  │
│  Angular (:4200) │   /api/...       │ Spring Boot      │           │   (:5432)    │
│                  │ ◀───────────────│      (:8080)     │ ◀────────│  Docker      │
└──────────────────┘                  └──────────────────┘           └──────────────┘
```

| Teil | Technologie | Ordner | Port |
|---|---|---|---|
| Frontend | Angular 22, Angular Material | [frontend/](frontend/) | 4200 |
| Backend | Java 25, Spring Boot 4 (Web MVC, Data JPA, Validation) | [backend/](backend/) | 8080 |
| Datenbank | PostgreSQL 18 (Docker) | [backend/compose.yaml](backend/compose.yaml) | 5432 |

### Backend

Das Backend ist nach Fachbereichen in Packages aufgeteilt (`subject`, `grade`). Innerhalb eines Packages gilt eine Schichtenarchitektur:

```
Controller  →  Service  →  Repository  →  Datenbank
(REST, Validierung)  (Logik)  (Spring Data JPA)
```

- **Controller**: nimmt HTTP-Requests entgegen und validiert die Eingabe.
- **Service**: enthält die Geschäftslogik (z. B. Duplikate prüfen).
- **Repository**: Zugriff auf die Datenbank via JPA. Die Tabellen werden beim Start automatisch erstellt (`ddl-auto=update`).
- **Request/Response-Records**: trennen die API-Objekte von den Entities.
- **CorsConfig**: erlaubt Anfragen von `http://localhost:4200` auf `/api/**`.

Weitere Details zum Package `subject` stehen in [backend/docs/subject-dokumentation.md](backend/docs/subject-dokumentation.md).

### REST-API

| Methode | Pfad | Beschreibung |
|---|---|---|
| `GET` | `/api/subjects` | Alle Fächer abrufen |
| `POST` | `/api/subjects` | Fach anlegen |
| `PUT` | `/api/subjects/{id}` | Fach ändern |
| `DELETE` | `/api/subjects/{id}` | Fach löschen |
| `GET` | `/api/grades` | Alle Noten abrufen (in Arbeit, noch nicht im Frontend) |
| `POST` | `/api/grades` | Note anlegen (in Arbeit, noch nicht im Frontend) |

### Frontend

Das Frontend ist eine Angular-App mit Standalone-Komponenten. Seiten:

| Route | Beschreibung |
|---|---|
| `/` | Startseite: Tabelle aller Fächer mit Notenschnitt, verlinkt auf Fächer und Noten |
| `/subjects` | Fächer anlegen, bearbeiten, löschen |
| `/grades` | Pro Fach mehrere Noten erfassen (Noten können nur angelegt werden, es gibt noch kein Update/Delete im Backend) |


Der `SubjectService` ([frontend/src/app/subjects/subject.service.ts](frontend/src/app/subjects/subject.service.ts)) spricht mit dem Backend.
Die Backend-URL steht in [frontend/src/environments/](frontend/src/environments/) (`apiUrl`, Standard: `http://localhost:8080`).

## Installation

### Voraussetzungen

- [JDK 25](https://adoptium.net/)
- [Node.js](https://nodejs.org/) (aktuelle LTS-Version) mit npm
- [Docker](https://docs.docker.com/get-docker/) mit Docker Compose (für die Datenbank)
- Git

### 1. Repository klonen

```bash
git clone https://gitlab.com/Rajethan-Ranjan/m324-grade-compass.git
cd m324-grade-compass
```

### 2. Datenbank starten (optional)

> **Dieser Schritt ist optional.** Beim Start des Backends (Schritt 3) startet Spring Boot (`spring-boot-docker-compose`)
> die Datenbank automatisch über `backend/compose.yaml`, sofern Docker läuft. Es reicht also, nur das Backend zu starten.

Wer die Datenbank trotzdem separat starten möchte (z. B. um sie unabhängig vom Backend laufen zu lassen):

```bash
cd backend
docker compose up -d
```

Das startet PostgreSQL auf Port 5432 mit diesen Standardwerten:

| Einstellung | Wert |
|---|---|
| Datenbank | `gcdb` |
| Benutzer | `gcdbUser` |
| Passwort | `gcdbPassword123!` |

### 3. Backend starten

```bash
cd backend
./mvnw spring-boot:run
```

Dabei wird auch die Datenbank gestartet, falls sie noch nicht läuft (siehe Schritt 2). Docker muss dafür laufen.

Das Backend läuft danach auf <http://localhost:8080>. Test: <http://localhost:8080/api/subjects> sollte `[]` liefern.

Die Datenbank-Verbindung kann bei Bedarf über Umgebungsvariablen angepasst werden: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Erlaubte CORS-Origins lassen sich mit der Property `app.cors.allowed-origins` ändern.

### 4. Frontend starten

In einem zweiten Terminal:

```bash
cd frontend
npm install
npm start
```

Das Frontend läuft danach auf <http://localhost:4200>.

## Tests

```bash
# Backend (nutzt eine In-Memory-H2-Datenbank, kein Docker nötig)
cd backend
./mvnw test

# Frontend (Vitest)
cd frontend
npm test

# Frontend Linting (ESLint, Warnungen brechen den Lauf nicht ab)
cd frontend
npm run lint
```

## Build

```bash
# Backend: ausführbares JAR in backend/target/
cd backend
./mvnw clean package

# Frontend: Build in frontend/dist/
cd frontend
npm run build
```

## SonarQube

Für die Code-Qualitätsanalyse gibt es einen lokalen SonarQube-Server (Community Edition). Er ist unabhängig von der
Anwendung und wird nur bei Bedarf gestartet:

```bash
docker compose -f local/compose.yaml up -d
```

SonarQube ist danach unter <http://localhost:9000> erreichbar (der erste Start dauert einen Moment).
Die Konfiguration steht in [local/compose.yaml](local/compose.yaml), die Daten liegen in Docker-Volumes.

> Das Projekt enthält bisher nur den SonarQube-Server. Eine Analyse-Konfiguration (Projekt, Token, Scanner)
> ist noch nicht eingerichtet.

## Aufräumen

```bash
# Datenbank stoppen (im Ordner backend/)
docker compose down

# Datenbank stoppen und alle Daten löschen
docker compose down -v

# SonarQube stoppen (Daten bleiben erhalten)
docker compose -f local/compose.yaml down
```
