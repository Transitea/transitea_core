# --- Etape 1 : Construction de l application ---
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Telechargement des dependances en cache separement pour optimiser les layers Docker
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

COPY src/ src/

RUN ./mvnw package -DskipTests -B --no-transfer-progress


# --- Etape 2 : Image d execution legere ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Creation d un utilisateur non-root pour la securite
RUN addgroup -S transitea && adduser -S transitea -G transitea

COPY --from=build /app/target/*.jar app.jar

# Creation du repertoire de cache pour les QR codes
RUN mkdir -p /var/transitea/cache/qrcodes \
    && chown -R transitea:transitea /var/transitea

USER transitea

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
