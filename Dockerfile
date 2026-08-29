# ============================================================
# STAGE 1 - BUILD THE CHECKOUT SERVICE
# ============================================================
# We use the full Java 21 JDK here because Maven needs
# the Java compiler to build the Spring Boot application.
# ============================================================

FROM eclipse-temurin:21-jdk-jammy AS build

# ------------------------------------------------------------
# Set /app as the working directory inside the build container.
# ------------------------------------------------------------

WORKDIR /app
# ------------------------------------------------------------
# Copy Maven Wrapper and pom.xml first.
#
# This helps Docker reuse cached dependency layers when only
# application source code changes.
# ------------------------------------------------------------

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# ------------------------------------------------------------
# Remove Windows CRLF characters from mvnw.
#
# This avoids errors such as:
# /bin/sh^M: bad interpreter
#
# chmod makes the Maven wrapper executable inside Linux.
# ------------------------------------------------------------

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# ------------------------------------------------------------
# Download Maven dependencies before copying source code.
#
# This makes repeated Docker builds faster because dependencies
# can remain cached when pom.xml has not changed.
# ------------------------------------------------------------

RUN ./mvnw \
    --batch-mode \
    --no-transfer-progress \
    dependency:go-offline
# ------------------------------------------------------------
# Copy Checkout Service Java source code and resources.
# ------------------------------------------------------------

COPY src/ src/
# ------------------------------------------------------------
# Build the Spring Boot executable JAR.
#
# clean     = removes previous build output
# package   = creates the JAR
# skipTests = GitHub Actions already runs tests before Docker
#
# Then:
# 1. Find the real executable JAR
# 2. Make sure a JAR was found
# 3. Rename/copy it to /app/app.jar
#
# The runtime image therefore does not need to know the Maven
# versioned filename such as checkoutservice-1.0.3.1.jar.
# ------------------------------------------------------------

RUN ./mvnw \
    --batch-mode \
    --no-transfer-progress \
    clean package \
    -DskipTests \
    && JAR_FILE="$(find target \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name 'original-*.jar' \
        ! -name '*-sources.jar' \
        ! -name '*-javadoc.jar' \
        | head -1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar
# ============================================================
# STAGE 2 - RUN THE CHECKOUT SERVICE
# ============================================================
# We no longer need Maven or the Java compiler.
#
# The smaller Java 21 JRE is enough to execute the final JAR.
# ============================================================

FROM eclipse-temurin:21-jre-jammy
# ------------------------------------------------------------
# Application directory inside the runtime container.
# ------------------------------------------------------------

WORKDIR /app
# ------------------------------------------------------------
# Create a dedicated non-root Linux user.
#
# Running the application as non-root reduces security risk.
# UID 10001 matches the Kubernetes securityContext used by
# our Checkout Service Helm chart.
# ------------------------------------------------------------

RUN useradd \
    --system \
    --uid 10001 \
    --no-create-home \
    appuser
# ------------------------------------------------------------
# Copy only the finished application JAR from the build stage.
#
# --chown makes UID 10001 the owner of the JAR.
# ------------------------------------------------------------

COPY --from=build \
    --chown=10001:10001 \
    /app/app.jar \
    /app/app.jar
# ------------------------------------------------------------
# Run the Checkout Service as the non-root application user.
# ------------------------------------------------------------

USER 10001
# ------------------------------------------------------------
# Checkout Service listens on port 8086.
#
# application.yml:
# server.port=${SERVER_PORT:8086}
#
# Helm values also use containerPort/service port 8086.
# ------------------------------------------------------------

EXPOSE 8086
# ------------------------------------------------------------
# Start the Spring Boot Checkout Service.
#
# UTC:
# Keeps application timestamps consistent.
#
# MaxRAMPercentage=75:
# Allows Java to use roughly 75% of container memory and leaves
# some memory for JVM/native/container overhead.
#
# /app/app.jar:
# Final Spring Boot executable JAR.
# ------------------------------------------------------------

ENTRYPOINT ["java","-Duser.timezone=UTC","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]