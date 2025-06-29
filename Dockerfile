# Dockerfile com New Relic Agent
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar gradle e curl para download do New Relic
RUN apk add --no-cache gradle curl unzip

# Copiar tudo de uma vez (sem otimização de cache)
COPY . .

# Build direto sem etapas intermediárias
RUN gradle clean bootJar --no-daemon

# Encontrar e renomear JAR
RUN find build/libs -name "*.jar" -exec cp {} app.jar \;

# Download do New Relic Agent
RUN curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && unzip newrelic-java.zip \
    && rm newrelic-java.zip

# Stage de produção
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instalar curl para healthchecks (opcional)
RUN apk add --no-cache curl

# Copiar JAR da aplicação
COPY --from=builder /app/app.jar app.jar

# Copiar New Relic Agent
COPY --from=builder /app/newrelic/ ./newrelic/

# Copiar configuração customizada do New Relic (se existir)
COPY newrelic.yml ./newrelic/newrelic.yml

# Criar diretório para logs
RUN mkdir -p /app/logs

# Configurar variáveis de ambiente padrão
ENV NEW_RELIC_APP_NAME="TourApp"
ENV NEW_RELIC_HIGH_SECURITY="false"
ENV NEW_RELIC_DISTRIBUTED_TRACING_ENABLED="true"
ENV NEW_RELIC_APPLICATION_LOGGING_ENABLED="true"
ENV NEW_RELIC_APPLICATION_LOGGING_FORWARDING_ENABLED="true"
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Expor portas
EXPOSE 8080

# Executar aplicação com New Relic Agent
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:./newrelic/newrelic.jar -Dserver.port=${PORT:-8080} -jar app.jar"]