# Multi-stage build otimizado para Railway
FROM eclipse-temurin:21-jdk-alpine AS builder

# Instalar dependências necessárias
RUN apk add --no-cache curl

# Definir diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração Gradle primeiro (cache de layers)
COPY gradle gradle/
COPY gradlew gradle.properties build.gradle settings.gradle ./

# Dar permissão de execução ao gradlew
RUN chmod +x ./gradlew

# Download de dependências (cache separado)
RUN ./gradlew dependencies --no-daemon

# Copiar código fonte
COPY src src/

# Build da aplicação
RUN ./gradlew bootJar --no-daemon --parallel

# Verificar se o JAR foi criado
RUN find build/libs -name "*.jar" -type f

# Stage de produção
FROM eclipse-temurin:21-jre-alpine

# Metadados
LABEL maintainer="Tour App Team"
LABEL version="1.0.0"

# Instalar curl para health checks
RUN apk add --no-cache curl

# Criar usuário não-root
RUN addgroup -g 1001 -S appgroup && \
    adduser -S -D -u 1001 -G appgroup appuser

# Definir diretório de trabalho
WORKDIR /app

# Copiar JAR do stage builder
COPY --from=builder /app/build/libs/*-boot.jar app.jar

# Criar diretório para arquivos
RUN mkdir -p files && chown -R appuser:appgroup /app

# Mudar para usuário não-root
USER appuser

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Variáveis de ambiente otimizadas para Railway
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Ponto de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=$PORT"]