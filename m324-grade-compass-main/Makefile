# Hier erstellt ihr alle Befehle damit die komplette Entwicklungsumgebung, installiert und gestartet werden kann
# Die einzelnen Schrite um die Anwedungen zu starten findet ihr im README.md

SONAR_HOST_URL ?= http://sonarqube:9000
SONAR_NETWORK ?= sonar-net
SONAR_TOKEN ?=

.PHONY: coverage linting docker docker-frontend docker-clean

coverage:
	mvn -f backend/pom.xml test
	mvn -f backend/pom.xml dependency:copy-dependencies -DoutputDirectory=target/dependency

linting: coverage
	docker run --rm -e SONAR_HOST_URL="$(SONAR_HOST_URL)" -e SONAR_TOKEN="$(SONAR_TOKEN)" --network $(SONAR_NETWORK) -v .:/usr/src sonarsource/sonar-scanner-cli

docker:
	docker build -t grade-compass-backend:latest -f backend/Dockerfile backend

docker-frontend:
	docker build -t grade-compass-frontend:latest -f frontend/Dockerfile frontend

docker-clean:
	docker compose -f backend/compose.yaml down
	-docker rm -f grade-compass-backend grade-compass-backend-v1 grade-compass-backend-v2 grade-compass-frontend
	-docker image rm -f grade-compass-backend:latest grade-compass-backend:1.0 grade-compass-backend:2.0 grade-compass-frontend:latest