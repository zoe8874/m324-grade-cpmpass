# Hier erstellt ihr alle Befehle damit die komplette Entwicklungsumgebung, installiert und gestartet werden kann
# Die einzelnen Schrite um die Anwedungen zu starten findet ihr im README.md

SONAR_HOST_URL ?= http://sonarqube:9000
SONAR_NETWORK ?= sonar-net
SONAR_TOKEN ?=

.PHONY: coverage linting

coverage:
	mvn -f backend/pom.xml test
	mvn -f backend/pom.xml dependency:copy-dependencies -DoutputDirectory=target/dependency

linting: coverage
	docker run --rm -e SONAR_HOST_URL="$(SONAR_HOST_URL)" -e SONAR_TOKEN="$(SONAR_TOKEN)" --network $(SONAR_NETWORK) -v .:/usr/src sonarsource/sonar-scanner-cli