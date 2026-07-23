# Build stage
FROM maven:3.8-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn package -DskipTests

# Run stage
FROM openjdk:11-jre-slim
WORKDIR /app

# Adiciona o Prometheus JMX Exporter para Advanced Logging/Monitoring
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.0/jmx_prometheus_javaagent-0.17.0.jar /app/jmx_exporter.jar
COPY prometheus.yml /app/prometheus-config.yml

COPY --from=build /app/target/*.war /app/l3mon.war

EXPOSE 8080
EXPOSE 9404

# Configura o agente Prometheus para monitoramento da JVM
ENTRYPOINT ["java", "-javaagent:/app/jmx_exporter.jar=9404:/app/prometheus-config.yml", "-jar", "/app/l3mon.war"]
