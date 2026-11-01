# ===================================================================
#  Multi-stage Docker build for the MoveMate API
#
#  Why two stages?
#    Stage 1 needs Maven + the full JDK + the whole ~200 MB dependency cache
#    to compile. None of that is needed to RUN the app. Stage 2 starts from a
#    clean JRE image and copies in only the finished jar, which takes the final
#    image from roughly 700 MB down to about 200 MB. Smaller image = faster
#    cold starts on Railway/Render free tiers.
#
#  Why Docker at all, instead of letting the platform auto-detect Maven?
#    The build becomes identical on your laptop, in CI and on the host. "Works
#    on my machine" stops being a thing, and you do not need the Maven wrapper.
# ===================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy ONLY the pom first and resolve dependencies. Docker caches each layer,
# so as long as pom.xml is unchanged this expensive download is reused on every
# later build - even when you edit Java files. Copying src first would throw the
# cache away on every single code change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now the source, which changes often and therefore comes last.
COPY src ./src

# -DskipTests because tests belong in CI, not in the release image build.
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user. If the app is ever compromised, the attacker lands as
# an unprivileged user inside the container rather than root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Wildcard copy so the jar name can change with the version without editing this file.
COPY --from=build /app/target/*.jar app.jar

# Documentation for humans and tooling; the platform still injects the real PORT.
EXPOSE 8080

# MaxRAMPercentage=75 tells the JVM to size the heap from the CONTAINER limit
# instead of the host's total RAM. Without it, a 512 MB free-tier container gets
# OOM-killed because the JVM thinks it has 8 GB to play with.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
