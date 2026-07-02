# Build context is the REPO ROOT (not server/), since the Maven build pulls in
# ../web via the frontend-maven-plugin (see server/pom.xml's build-web profile)
# to produce the embedded static site. Build with:
#   docker build -f server/Dockerfile -t openbake-server .

# ── Stage 1: build (Maven + embedded Next.js static export) ──────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY web ./web
COPY server ./server

WORKDIR /build/server
RUN mvn -B -DskipTests package

# ── Stage 2: runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/server/target/openbake-server.jar ./openbake-server.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "openbake-server.jar"]
