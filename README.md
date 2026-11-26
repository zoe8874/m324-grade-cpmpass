Aufgabe02


1.1 : Entpacke das ZIP-File und führe im Verzeichnis backend die Tests mit mvn test aus. Wie viele Tests werden ausgeführt? Wie viele schlagen fehl?

 Results:
[INFO] 
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------

Antwort: Es werden 44 Tests ausgeführt und davon schlägt keiner fehl.

Aufgabe04
```
Abhängigkeiten verstehen

- Fehlen Laufzeit-Abhängigkeiten, kann die Anwendung nicht starten oder Funktionen wie Datenbankzugriffe nicht ausführen. Fehlende Build- ober Test-Abhängigkeiten verhindern Kompilierung bzw. Tests; inkompatible Versionen können zusätzlich Laufzeitfehler verursachen.
- Backend: pom.xml deklariert 8 direkte Abhängigkeiten. mvnw dependency:tree zeigt 91 Einträge inklusive transitiver Abhängigkeiten.
- Die Versionen der Spring-Abhängigkeiten kommen vom Parent spring-boot-starter-parent (hier 4.1.2); dessen Dependency Management verwaltet kompatible Versionen.
- runtime bedeutet: zur Laufzeit verfügbar, nicht für die Kompilierung benötigt. Der PostgreSQL-JDBC-Treiber wird zur Laufzeit gebraucht. test ist nur für Tests verfügbar; H2 ersetzt dort PostgreSQL als In-Memory-Datenbank.
- Im Frontend sind dependencies für den Betrieb der App vorgesehen, devDependencies für Entwicklung, Test und Build. package-lock.json hält die konkret aufgelösten Versionen fest. npm install kann den Lockfile aktualisieren; npm ci installiert exakt daraus, entfernt vorher node_modules und schlägt bei Abweichungen fehl.

Terminal-Ausgabe_Kurz:

[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
LIBRARIES=91


- Backend: Das Multi-Stage-Dockerfile baut mit Maven/JDK und kopiert nur das ausführbare JAR in ein kleineres JRE-Image. Build-Werkzeuge und Maven-Abhängigkeiten gelangen so nicht ins Laufzeit-Image. .dockerignore schliesst target` aus.
- Datenbank und Backend müssen dasselbe Compose-Netzwerk verwenden: standardmässig  backend_default. Im Container ist localhost der Backend-Container selbst; als Host für DB_URL dient der Compose-Service postgres, z. B. jdbc:postgresql://postgres:5432/gcdb.
- Nach Änderung von Java-Code wird der Abhängigkeits-Layer wiederverwendet; Quelltext kopieren und Maven-Paket bauen müssen erneut laufen. Eine Änderung an pom.xml invalidiert auch den Layer dependency:go-offline, daher werden Abhängigkeiten neu geprüft/geladen und der Build dauert länger.
- Version 1.0 akzeptiert Namen bis 100 Zeichen; Version 2.0 begrenzt sie auf 50. Ein 60-Zeichen-Name wird daher von 1.0 akzeptiert und von 2.0 mit HTTP 400 abgelehnt. Rollback: den fehlerhaften Container stoppen und das Image grade-compass-backend:1.0 wieder starten.
- Gemeinsame Schemaänderungen können die jeweils andere Version brechen und einen Rollback erschweren. Änderungen sollten rückwärtskompatibel und schrittweise (zuerst erweitern, später entfernen/umbenennen) migriert werden.
- Das Frontend wirt mit Node gebaut und durch nginx ausgeliefert. nginx muss unbekannte Pfade auf index.html zurückführen (try_files), damit Angular-Routen wie /subjects und /grades beim direkten Aufruf funktionieren.

Build-Befehle: make docker und make docker frontend; Images und Build-Grössen lassen sich mit docker images vergleichen. make docker-clean entfernt die zugehörigen Container und Images.

Weitere überprüfte Ausgabe:


Backend-Tests nach der 50 Zeichen Regel: 
53 Tests, 0 Fehler, BUILD SUCCESS
grade-compass-backend:1.0   599MB
```
