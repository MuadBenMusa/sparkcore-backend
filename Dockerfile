# ============================================================
# SparkCore Backend – Multi-Stage Dockerfile
#
# Stage 1 (builder): kompiliert den Code und erstellt das JAR
# Stage 2 (runtime): schlankes Image nur mit dem fertigen JAR
#
# Build:   docker build -t sparkcore-backend .
# Run:     docker run -p 8080:8080 --env-file .env sparkcore-backend
# ============================================================

# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

# Arbeitsverzeichnis im Container
WORKDIR /app

# Maven Wrapper & pom.xml zuerst kopieren (besseres Layer-Caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ausführungsrechte setzen (nötig, da mvnw auf Windows ohne +x-Bit committed wird)
RUN chmod +x ./mvnw

# Nur Abhängigkeiten herunterladen (wird gecacht, solange pom.xml gleich bleibt)
RUN ./mvnw dependency:go-offline -q

# Quellcode kopieren und JAR bauen (ohne Tests – die laufen in CI)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Kein Root-User – Sicherheits-Best-Practice
RUN addgroup -S sparkcore && adduser -S sparkcore -G sparkcore
USER sparkcore

# Nur das fertige JAR aus Stage 1 kopieren
COPY --from=builder /app/target/*.jar app.jar

# Port freigeben (muss mit server.port in application.yaml übereinstimmen)
EXPOSE 8080

# Gesundheitscheck – Docker weiß, ob die App wirklich läuft
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# App starten
ENTRYPOINT ["java", "-jar", "app.jar"]
