# Dokumentation: Package `subject`

Diese Dokumentation beschreibt das Package `ch.bbzw.m324.grade_compass.subject` der Anwendung **Grade Compass**
(Teil 1) und die zugehörigen Tests (Teil 2).

- Quellcode: `backend/src/main/java/ch/bbzw/m324/grade_compass/subject/`
- Tests: `backend/src/test/java/ch/bbzw/m324/grade_compass/subject/`

---

# Teil 1: Anwendung (`src/main`)

## 1.1 Überblick

Das Package `subject` verwaltet **Schulfächer** (z. B. "Mathe", "Deutsch"). Fächer sind die Grundlage für
spätere Funktionen wie Noten und Durchschnittsberechnung. Aktuell gibt es zwei Funktionen:

| Funktion | HTTP-Aufruf | Ergebnis |
|---|---|---|
| Fach anlegen | `POST /api/subjects` | `201 Created` mit dem angelegten Fach |
| Alle Fächer anzeigen | `GET /api/subjects` | `200 OK` mit einer JSON-Liste |

### Schichtenarchitektur

Die Klassen sind in Schichten aufgebaut. Jede Schicht kennt nur die Schicht direkt darunter:

```
HTTP-Request (JSON)
      │
      ▼
┌────────────────────┐   nimmt Requests an, validiert die Eingabe (@Valid),
│ SubjectController  │   gibt HTTP-Statuscodes zurück
└─────────┬──────────┘
          │  CreateSubjectRequest  (rein)      SubjectResponse (raus)
          ▼
┌────────────────────┐   Geschäftslogik: Namen trimmen, Duplikate prüfen
│  SubjectService    │
└─────────┬──────────┘
          │  Subject (Entity)
          ▼
┌────────────────────┐   Zugriff auf die Datenbank (Spring Data JPA)
│ SubjectRepository  │
└─────────┬──────────┘
          ▼
   Tabelle "subject" (PostgreSQL, in Tests H2)
```

### Ablauf: Fach anlegen (`POST /api/subjects`)

1. Der Client sendet `{"name": "  Mathe  "}`.
2. Spring wandelt das JSON in ein `CreateSubjectRequest` um.
3. `@Valid` prüft die Regeln des Records (nicht leer, höchstens 100 Zeichen).
   Bei einem Verstoss antwortet Spring sofort mit `400 Bad Request`, der Service wird nicht aufgerufen.
4. `SubjectService.create` entfernt Leerzeichen am Anfang und Ende (`trim`) → `"Mathe"`.
5. Der Service fragt `existsByNameIgnoreCase("Mathe")`. Existiert das Fach schon (egal ob "mathe" oder "MATHE"),
   wird eine `ResponseStatusException` mit `409 Conflict` geworfen.
6. Sonst wird ein neues `Subject` gespeichert, und die Datenbank vergibt die ID.
7. Der Controller antwortet mit `201 Created` und `{"id": 1, "name": "Mathe"}`.

### Ablauf: Fächer anzeigen (`GET /api/subjects`)

Der Controller ruft `service.findAll()` auf. Der Service lädt alle `Subject`-Entities, wandelt sie in
`SubjectResponse` um und gibt die Liste zurück (leere Liste, wenn es keine Fächer gibt).

---

## 1.2 Die Klassen im Detail

### `Subject` – die Entity (Datenbank-Modell)

```java
@Entity
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Subject(){}
    public Subject(String name){ this.name = name; }

    public Long getId(){return Id;}
    public String getName(){return name;}
}
```

| Element | Bedeutung |
|---|---|
| `@Entity` | Markiert die Klasse als JPA-Entity: Hibernate bildet sie auf eine Datenbanktabelle ab. |
| `@Table(name = "subject")` | Name der Tabelle in der Datenbank. |
| `@Id` | Das Feld ist der Primärschlüssel. |
| `@GeneratedValue(strategy = IDENTITY)` | Die Datenbank vergibt die ID selbst (Auto-Increment-Spalte). Die ID ist erst nach dem Speichern bekannt. |
| `@Column(nullable = false, unique = true)` | Die Spalte `name` darf nicht `NULL` sein, und jeder Name darf nur einmal vorkommen (Unique-Constraint). |
| `protected Subject(){}` | Parameterloser Konstruktor, den JPA zwingend braucht. Er ist `protected`, damit er im eigenen Code nicht versehentlich benutzt wird. |
| `public Subject(String name)` | Konstruktor für den eigenen Code. Es gibt bewusst keine Setter, die Entity ist nach dem Erstellen unveränderlich. |

**Hinweise:**
- Das Feld heisst `Id` (gross) und ist ein primitives `long`. Der Getter gibt `Long` zurück. Funktional ist das in
  Ordnung, üblich wäre `private Long id`. Wegen des primitiven Typs hat ein noch nicht gespeichertes `Subject`
  die ID `0` statt `null`.
- Die Länge 100 aus `CreateSubjectRequest` ist in der DB nicht abgebildet: Hibernate legt die Spalte als
  `varchar(255)` an.

### `SubjectRepository` – Datenzugriff

```java
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsByNameIgnoreCase(String name);
}
```

- `JpaRepository<Subject, Long>`: Spring Data erzeugt zur Laufzeit automatisch eine Implementierung. Es gibt
  fertige Methoden wie `save`, `findAll`, `findById`, `count`, `deleteAll`. `Subject` ist die Entity,
  `Long` der Typ der ID.
- `existsByNameIgnoreCase(String name)`: **Derived Query**. Spring leitet die SQL-Abfrage aus dem Methodennamen ab:
  `exists` → Ja/Nein-Abfrage, `ByName` → Filter auf das Feld `name`, `IgnoreCase` → Gross-/Kleinschreibung wird
  ignoriert. Intern entsteht ungefähr `select id from subject where upper(name) = upper(?) fetch first 1 rows only`.

### `CreateSubjectRequest` – Eingabe-DTO

```java
public record CreateSubjectRequest(
        @NotBlank @Size(max = 100) String name
) {}
```

- `record`: Kurzform für eine unveränderliche Datenklasse. Java erzeugt automatisch Konstruktor,
  Zugriffsmethode `name()`, `equals`, `hashCode` und `toString`.
- `@NotBlank`: Der Wert darf nicht `null`, leer (`""`) oder nur aus Leerzeichen/Tabs/Zeilenumbrüchen bestehen.
- `@Size(max = 100)`: Höchstens 100 Zeichen. Gemessen wird der Originalwert **vor** dem `trim` des Services.
- Warum ein eigenes DTO (Data Transfer Object)? Die Entity bleibt von der API getrennt. Der Client kann
  z. B. keine ID mitschicken.

### `SubjectResponse` – Ausgabe-DTO

```java
public record SubjectResponse(Long id, String name) {
    static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }
}
```

- Das ist die Darstellung eines Fachs nach aussen (JSON: `{"id": 1, "name": "Mathe"}`).
- `from(Subject)`: statische Fabrikmethode, die eine Entity in ein DTO übersetzt. Sie ist package-private
  (ohne `public`) und nur innerhalb von `subject` sichtbar.

### `SubjectService` – Geschäftslogik

```java
@Service
public class SubjectService {
    private final SubjectRepository repository;
    public SubjectService(SubjectRepository repository) { this.repository = repository; }

    public SubjectResponse create(CreateSubjectRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists: " + name);
        }
        return SubjectResponse.from(repository.save(new Subject(name)));
    }

    public List<SubjectResponse> findAll() {
        return repository.findAll().stream().map(SubjectResponse::from).toList();
    }
}
```

| Element | Bedeutung |
|---|---|
| `@Service` | Spring erzeugt die Klasse als Bean und verwaltet sie. |
| Konstruktor mit `SubjectRepository` | **Constructor Injection**: Spring übergibt das Repository automatisch. Das Feld ist `final`, und die Klasse lässt sich in Tests leicht mit einem Mock bauen. |
| `create` | 1. `trim()` 2. Duplikatprüfung 3. `save` 4. Entity in `SubjectResponse` umwandeln. |
| `ResponseStatusException(CONFLICT, …)` | Spring wandelt diese Exception automatisch in die HTTP-Antwort `409 Conflict` um. |
| `findAll` | `repository.findAll()` liefert `List<Subject>`. `stream().map(SubjectResponse::from).toList()` wandelt jedes Element um. |

**Bekannte Einschränkung (Race Condition):** Die Prüfung `existsBy…` und das `save` sind nicht atomar.
Senden zwei Clients gleichzeitig denselben neuen Namen, können beide die Prüfung bestehen. Der zweite `save`
scheitert dann an der DB-Constraint mit einem Fehler (500) statt mit 409. Ein möglicher Fix: die
`DataIntegrityViolationException` fangen und in 409 umwandeln.

### `SubjectController` – REST-Schnittstelle

```java
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService service;
    public SubjectController(SubjectService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<SubjectResponse> list() { return service.findAll(); }
}
```

| Annotation | Bedeutung |
|---|---|
| `@RestController` | Controller, dessen Rückgabewerte direkt als JSON in den Response-Body geschrieben werden. |
| `@RequestMapping("/api/subjects")` | Basis-Pfad für alle Methoden der Klasse. |
| `@PostMapping` / `@GetMapping` | Verknüpft die Methode mit HTTP `POST` bzw. `GET` auf dem Basis-Pfad. |
| `@ResponseStatus(CREATED)` | Setzt bei Erfolg den Status `201` statt `200`. |
| `@RequestBody` | Der JSON-Body des Requests wird in ein `CreateSubjectRequest` umgewandelt. |
| `@Valid` | Löst die Bean-Validation aus (`@NotBlank`, `@Size`). Bei Fehlern wirft Spring `MethodArgumentNotValidException` → `400`. |

**Fehlerantworten im Überblick:**

| Situation | Status | Woher |
|---|---|---|
| Erfolgreich angelegt | 201 | `@ResponseStatus` |
| Name leer, fehlend, nur Leerzeichen oder > 100 Zeichen | 400 | `@Valid` |
| Body ist kein gültiges JSON | 400 | Spring (`HttpMessageNotReadableException`) |
| Fach existiert schon (auch anderer Schreibweise) | 409 | `ResponseStatusException` im Service |
| Gleichzeitiges Anlegen desselben Namens (Race Condition) | 500 | DB-Constraint, nicht abgefangen |

---

## 1.3 Konfiguration

| Datei | Zweck |
|---|---|
| `src/main/resources/application.properties` | Produktiv/lokal: PostgreSQL. `DB_URL`, `DB_USER`, `DB_PASSWORD` kommen aus Umgebungsvariablen und haben lokale Standardwerte. `ddl-auto=update` legt/ändert die Tabelle `subject` automatisch an. |
| `src/test/resources/application.properties` | Tests: H2-Datenbank im Arbeitsspeicher, `ddl-auto=create-drop` (Tabelle wird beim Start erzeugt und am Ende gelöscht). |
| `local/compose.yaml` | Startet PostgreSQL 18 lokal: `docker compose -f ../local/compose.yaml up -d`. |

---

# Teil 2: Tests (`src/test`)

## 2.1 Überblick der Testarten

| Testklasse | Testart | Was läuft wirklich? | Was ist ersetzt? |
|---|---|---|---|
| `CreateSubjectRequestTest` | **Unit-Test** | Nur Bean-Validation (Hibernate Validator) | Kein Spring |
| `SubjectServiceTest` | **Unit-Test** | Nur `SubjectService` | Repository ist ein Mock |
| `SubjectRepositoryTest` | **Slice-/Integrationstest** (Persistenzschicht) | Spring Data JPA, Hibernate, H2-Datenbank | Controller/Service existieren nicht |
| `SubjectControllerTest` | **Slice-/Integrationstest** (Webschicht) | Spring MVC, JSON, Validierung, Fehlerbehandlung | Service ist ein Mock, keine DB |
| `SubjectIntegrationTest` | **Integrationstest** (End-to-End) | Controller → Service → Repository → H2 | Nur der HTTP-Server (MockMvc statt echtem Port) |
| `GradeCompassApplicationTests` | **Smoke-Test** | Gesamter Spring-Context startet | – |

**Begriffe:**
- **Unit-Test:** Testet eine einzelne Klasse isoliert, ohne Spring und ohne Datenbank. Sehr schnell. Abhängigkeiten
  werden durch Mocks ersetzt.
- **Slice-Test:** Spring startet nur einen *Ausschnitt* (Slice) der Anwendung, z. B. nur die Web- oder nur die
  JPA-Schicht. Schneller als ein voller Test, aber mit echtem Spring-Verhalten.
- **Integrationstest:** Mehrere Schichten arbeiten zusammen, hier alle Schichten mit einer echten (H2-)Datenbank.
- **Mock:** Ein Stellvertreter-Objekt, dessen Verhalten der Test vorgibt und das aufgezeichnet, wie es aufgerufen wurde.

**Testaufbau (Given–When–Then):** Jeder Test bereitet etwas vor (given), führt eine Aktion aus (when) und prüft das
Ergebnis (then). Die Methodennamen folgen dem Muster `methode_situation_erwartetesErgebnis`,
z. B. `create_duplicateName_throwsConflictAndDoesNotSave`.

**Tests ausführen:**
```bash
./mvnw test                              # alle Tests
./mvnw test -Dtest=SubjectServiceTest    # nur eine Klasse
./mvnw test -Dtest='SubjectServiceTest#create_trimsNameBeforeCheckingAndSaving'   # nur ein Test
```

---

## 2.2 `CreateSubjectRequestTest` (Unit-Test, 8 Tests)

Prüft die Validierungsregeln des Records **ohne Spring**: Mit der Jakarta-Validation-API wird direkt ein
`Validator` gebaut und auf ein Objekt angewendet.

**Aufbau:**
- `@BeforeAll static void setUp()` erzeugt einmalig eine `ValidatorFactory` und einen `Validator`.
- `@AfterAll static void tearDown()` schliesst die Factory.

**Tests:**

| Test | Prüft |
|---|---|
| `validName_hasNoViolations` | "Mathematik" ist gültig → keine Verstösse. |
| `nullEmptyOrBlankName_isRejected` | Parametrisiert (siehe unten): `null`, `""`, `" "`, `"   "`, Tab, Zeilenumbruch → mindestens ein Verstoss auf dem Feld `name`. Das sind 6 Testläufe. |
| `nameWithExactly100Characters_isAccepted` | Grenzwert: genau 100 Zeichen sind erlaubt. |
| `nameWith101Characters_isRejected` | Grenzwert: 101 Zeichen → genau ein Verstoss auf `name`. |

**Keywords und Funktionen:**

| Element | Bedeutung |
|---|---|
| `@Test` | Markiert eine Methode als Test (JUnit 5). |
| `@ParameterizedTest` | Derselbe Test läuft mehrmals mit verschiedenen Eingabewerten. |
| `@NullAndEmptySource` | Liefert zusätzlich `null` und `""` als Parameter. |
| `@ValueSource(strings = {…})` | Liefert die aufgelisteten Strings als Parameter. |
| `@BeforeAll` / `@AfterAll` | Wird einmal vor / nach allen Tests der Klasse ausgeführt (Methode muss `static` sein). |
| `Validation.buildDefaultValidatorFactory()` | Erzeugt die Factory für Bean-Validation. |
| `validator.validate(obj)` | Prüft das Objekt und gibt ein `Set<ConstraintViolation<…>>` zurück (leer = gültig). |
| `ConstraintViolation.getPropertyPath()` | Zeigt, welches Feld den Verstoss ausgelöst hat (hier `name`). |
| `"a".repeat(100)` | Erzeugt einen String aus 100 mal "a" für die Grenzwerttests. |
| `assertThat(x).isEmpty()` / `.isNotEmpty()` / `.hasSize(n)` | AssertJ-Prüfungen auf Collections. |
| `allSatisfy(…)` | Jedes Element muss die Bedingung erfüllen. |

---

## 2.3 `SubjectServiceTest` (Unit-Test, 6 Tests)

Testet die **Geschäftslogik** des Services isoliert. Das Repository ist ein Mock, es gibt weder Spring noch
Datenbank. Der Test läuft in Millisekunden.

**Aufbau:**
```java
@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {
    @Mock private SubjectRepository repository;
    @InjectMocks private SubjectService service;
```
- `@ExtendWith(MockitoExtension.class)`: Aktiviert Mockito in JUnit 5. Mockito arbeitet im Strict-Modus: Ein
  eingerichteter, aber nie benutzter Stub lässt den Test fehlschlagen (`UnnecessaryStubbingException`).
- `@Mock`: Erzeugt ein Mock-Objekt des Repositorys.
- `@InjectMocks`: Erzeugt den echten `SubjectService` und übergibt ihm den Mock über den Konstruktor.

**Tests:**

| Test | Prüft |
|---|---|
| `create_savesSubjectAndReturnsResponse` | Kein Duplikat → `save` wird aufgerufen, Response enthält den Namen. |
| `create_trimsNameBeforeCheckingAndSaving` | `"  Mathe  "` wird zu `"Mathe"`. Sowohl die Prüfung als auch das Speichern verwenden den getrimmten Namen. |
| `create_duplicateName_throwsConflictAndDoesNotSave` | Existiert der Name → `ResponseStatusException` mit Status 409, Nachricht enthält "Mathe", `save` wird nie aufgerufen. |
| `create_duplicateDetectedAfterTrimming_throwsConflict` | `"  Mathe "` wird getrimmt und dann als Duplikat erkannt → 409. |
| `findAll_mapsEntitiesToResponses` | Zwei Entities werden in der gleichen Reihenfolge zu zwei Responses. |
| `findAll_returnsEmptyListWhenNoSubjectsExist` | Leere Liste bleibt leer. |

**Keywords und Funktionen:**

| Element | Bedeutung |
|---|---|
| `when(repository.xyz(..)).thenReturn(wert)` | **Stubbing**: Legt fest, was der Mock bei diesem Aufruf zurückgibt. |
| `.thenAnswer(inv -> inv.getArgument(0))` | Der Mock gibt das Argument zurück, mit dem er aufgerufen wurde. So verhält sich `save` wie ein echtes Repository. |
| `any(Subject.class)` | Argument-Matcher: passt auf jedes `Subject`. |
| `verify(mock).methode(..)` | Prüft, dass die Methode genau einmal aufgerufen wurde. |
| `verify(mock, never()).save(any())` | Prüft, dass die Methode **nie** aufgerufen wurde. |
| `ArgumentCaptor<Subject>` + `captor.capture()` / `getValue()` | Fängt das Objekt ab, das an `save` übergeben wurde, damit man dessen Inhalt (getrimmter Name) prüfen kann. |
| `assertThatThrownBy(() -> …)` | Prüft, dass der Code eine Exception wirft. |
| `isInstanceOfSatisfying(Typ.class, e -> …)` | Prüft den Exception-Typ und erlaubt weitere Prüfungen am Objekt (hier den Statuscode). |
| `hasMessageContaining("Mathe")` | Die Fehlermeldung enthält den Text. |
| `extracting(SubjectResponse::name)` + `containsExactly(..)` | Liest ein Feld aus jedem Element und prüft Inhalt **und** Reihenfolge. |

---

## 2.4 `SubjectRepositoryTest` (Slice-/Integrationstest, 8 Tests)

Testet das Repository gegen eine **echte Datenbank** (H2 im Speicher). Geprüft werden auch die Constraints aus
der Entity (`unique`, `nullable = false`) und die abgeleitete Abfrage `existsByNameIgnoreCase`.

**Aufbau:**
```java
@DataJpaTest
class SubjectRepositoryTest {
    @Autowired private SubjectRepository repository;
```
- `@DataJpaTest`: Startet nur die JPA-Schicht (Entities, Repositories, Hibernate, Datenbank). Controller und
  Services werden nicht geladen. Jeder Test läuft in einer **Transaktion, die danach zurückgerollt wird**, die Tests
  beeinflussen sich also nicht. Die konfigurierte DB wird durch eine eingebettete Datenbank ersetzt.
- `@Autowired`: Spring injiziert die echte Repository-Instanz.

**Tests:**

| Test | Prüft |
|---|---|
| `save_assignsIdAndPersistsName` | Nach dem Speichern hat das Fach eine positive ID, und `findById` liefert es mit dem richtigen Namen zurück. |
| `existsByNameIgnoreCase_isTrueForSameName` | Gleicher Name → `true`. |
| `existsByNameIgnoreCase_ignoresCase` | "mathe" und "MATHE" finden "Mathe". |
| `existsByNameIgnoreCase_isFalseForUnknownName` | Unbekannter Name → `false`. |
| `existsByNameIgnoreCase_isFalseOnEmptyTable` | Leere Tabelle → `false`. |
| `save_duplicateName_violatesUniqueConstraint` | Derselbe Name zweimal → `DataIntegrityViolationException`. |
| `save_nameDifferingOnlyInCase_isNotBlockedByDatabaseConstraint` | **Dokumentiert eine Einschränkung:** Die DB-Constraint ist case-sensitive, "Mathe" und "mathe" können nebeneinander existieren. Nur der Service verhindert das. |
| `save_nullName_violatesNotNullConstraint` | `null` als Name → `DataIntegrityViolationException`. |

**Keywords und Funktionen:**

| Element | Bedeutung |
|---|---|
| `saveAndFlush(entity)` | Speichert und schreibt die Änderung **sofort** in die DB. So tritt eine Constraint-Verletzung innerhalb des Tests auf und nicht erst beim späteren Commit. |
| `findById(id)` | Liefert ein `Optional<Subject>`. |
| `count()` | Anzahl der Zeilen in der Tabelle. |
| `DataIntegrityViolationException` | Spring-Exception, die bei Verstössen gegen DB-Constraints (unique, not null) geworfen wird. |
| `assertThat(optional).get().extracting(..)` | AssertJ: `get()` packt den Inhalt des Optionals aus. `extracting` liest ein Feld. |
| `isNotNull()`, `isPositive()`, `isEqualTo(..)`, `isTrue()`, `isFalse()` | AssertJ-Standardprüfungen. |

---

## 2.5 `SubjectControllerTest` (Slice-/Integrationstest, 9 Tests)

Testet die **Webschicht** isoliert: URL-Zuordnung, JSON-Umwandlung, Validierung und Statuscodes. Der Service
ist ein Mock, es wird keine Datenbank benötigt.

**Aufbau:**
```java
@WebMvcTest(SubjectController.class)
class SubjectControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private SubjectService service;
```
- `@WebMvcTest(SubjectController.class)`: Startet nur die Spring-MVC-Schicht für diesen einen Controller
  (Validierung, JSON, Fehlerbehandlung). Services und Repositories werden nicht geladen.
- `MockMvc`: Simuliert HTTP-Requests, ohne einen echten Server zu starten.
- `@MockitoBean`: Ersetzt die Bean `SubjectService` im Spring-Context durch einen Mockito-Mock. Das ist der
  Nachfolger von `@MockBean` ab Spring Boot 4 / Spring Framework 7.

**Tests:**

| Test | Prüft |
|---|---|
| `post_validBody_returns201AndCreatedSubject` | Gültiger Body → 201, JSON mit `id` und `name`. Der Service erhält `new CreateSubjectRequest("Mathe")`. |
| `post_blankName_returns400AndDoesNotCallService` | Nur Leerzeichen → 400, der Service wird nicht aufgerufen. |
| `post_emptyName_returns400` | Leerer String → 400. |
| `post_missingName_returns400` | `{}` ohne Feld `name` → 400. |
| `post_nameLongerThan100Characters_returns400` | 101 Zeichen → 400. |
| `post_malformedJson_returns400` | Kaputtes JSON → 400, Service nicht aufgerufen. |
| `post_duplicateName_returns409` | Der Service-Mock wirft `ResponseStatusException(CONFLICT)` → Antwort 409. |
| `get_returnsAllSubjects` | Liste mit zwei Fächern → 200, Array mit 2 Elementen, Felder `id` und `name` stimmen. |
| `get_withoutSubjects_returnsEmptyArray` | Keine Fächer → 200 mit leerem Array. |

**Keywords und Funktionen:**

| Element | Bedeutung |
|---|---|
| `mockMvc.perform(request)` | Führt den simulierten HTTP-Request aus. |
| `post("/api/subjects")` / `get(..)` | Baut einen POST- bzw. GET-Request. |
| `.contentType(MediaType.APPLICATION_JSON)` | Setzt den Header `Content-Type: application/json`. |
| `.content("{…}")` | Setzt den Request-Body. |
| `.andExpect(status().isCreated())` | Erwartet Status 201. Weitere: `isOk()` (200), `isBadRequest()` (400), `isConflict()` (409). |
| `.andExpect(jsonPath("$.name").value("Mathe"))` | Prüft einen Wert im JSON. `$` ist die Wurzel, `$[0].name` das Feld `name` des ersten Array-Elements, `$.length()` die Array-Länge. |
| `verify(service).create(new CreateSubjectRequest("Mathe"))` | Prüft, dass der Service mit genau diesem Request aufgerufen wurde. Das klappt, weil Records `equals` automatisch implementieren. |
| `verifyNoInteractions(service)` | Der Service wurde überhaupt nicht angefasst. Beweist, dass die Validierung vorher gegriffen hat. |
| `thenThrow(exception)` | Der Mock wirft die angegebene Exception. |

---

## 2.6 `SubjectIntegrationTest` (Integrationstest, 7 Tests)

Testet die **gesamte Kette** vom HTTP-Request bis in die Datenbank, ohne Mocks. Hier zeigt sich, ob alle Schichten
zusammenpassen.

**Aufbau:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class SubjectIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private SubjectRepository repository;

    @BeforeEach
    void cleanDatabase() { repository.deleteAll(); }
```
- `@SpringBootTest`: Startet den **kompletten** Spring-Context der Anwendung (Controller, Service, Repository, H2).
- `@AutoConfigureMockMvc`: Stellt `MockMvc` bereit. Es wird kein echter Port geöffnet.
- `@BeforeEach cleanDatabase`: Löscht vor jedem Test alle Fächer. Dieser Test läuft **nicht** in einer
  zurückgerollten Transaktion (anders als `@DataJpaTest`), deshalb würden sich Tests sonst gegenseitig beeinflussen.
- Die Hilfsmethode `createSubject(name)` legt ein Fach per POST an und prüft dabei auf 201.

**Tests:**

| Test | Prüft |
|---|---|
| `createThenList_showsCreatedSubject` | POST und danach GET: Das Fach erscheint in der Liste. |
| `list_onEmptyDatabase_returnsEmptyArray` | Leere DB → leeres Array. |
| `create_duplicateName_returns409AndStoresOnlyOne` | Zweiter POST mit gleichem Namen → 409, in der DB bleibt genau 1 Eintrag. |
| `create_duplicateNameWithDifferentCase_returns409` | "Mathe" und danach "MATHE" → 409. Prüft das Zusammenspiel von Service und `existsByNameIgnoreCase`. |
| `create_nameWithSurroundingWhitespace_isStoredTrimmed` | `"  Mathe  "` wird als "Mathe" gespeichert. Die Antwort und die DB bestätigen das. |
| `create_blankName_returns400AndStoresNothing` | Ungültiger Name → 400, die DB bleibt leer. |
| `create_multipleSubjects_areAllListed` | Drei Fächer anlegen → GET liefert drei. |

**Keywords und Funktionen:** Zusätzlich zu den MockMvc-Funktionen aus 2.5 gelten:

| Element | Bedeutung |
|---|---|
| `@BeforeEach` | Wird vor **jedem** einzelnen Test ausgeführt. |
| `repository.deleteAll()` | Leert die Tabelle zwischen den Tests. |
| `repository.count()` | Prüft direkt in der DB, wie viele Einträge existieren. |
| `jsonPath("$.id").isNumber()` | Prüft, dass die ID eine Zahl ist (der konkrete Wert wird von der DB vergeben). |
| `assertThat(..).isZero()` | AssertJ: Wert ist 0. |

---

## 2.7 `GradeCompassApplicationTests` (Smoke-Test, 1 Test)

```java
@SpringBootTest
class GradeCompassApplicationTests {
    @Test void contextLoads() {}
}
```
Der Test ist leer. Er scheitert aber, wenn der Spring-Context nicht startet (z. B. wegen fehlender Beans oder
falscher Konfiguration). Er gilt als Basis-Absicherung ("läuft die App überhaupt?").

---

## 2.8 Wo liegt was? – Zuordnung Funktion → Test

| Funktion in der Anwendung | Abgesichert durch |
|---|---|
| Validierung des Namens (`@NotBlank`, `@Size`) | `CreateSubjectRequestTest` (direkt), `SubjectControllerTest` (über HTTP 400), `SubjectIntegrationTest` |
| Trimmen des Namens | `SubjectServiceTest`, `SubjectIntegrationTest` |
| Duplikatprüfung / 409 | `SubjectServiceTest`, `SubjectControllerTest`, `SubjectIntegrationTest` |
| `existsByNameIgnoreCase` | `SubjectRepositoryTest`, indirekt `SubjectIntegrationTest` |
| DB-Constraints (`unique`, `nullable = false`) | `SubjectRepositoryTest` |
| JSON-Format, Statuscodes, URLs | `SubjectControllerTest`, `SubjectIntegrationTest` |
| Zusammenspiel aller Schichten | `SubjectIntegrationTest`, `GradeCompassApplicationTests` |

**Nicht abgedeckt:** Die Race Condition bei gleichzeitigen Requests (siehe 1.2, `SubjectService`) und
`SubjectResponse.from` als eigene Einheit (wird indirekt in allen Service- und Integrationstests mitgeprüft).
