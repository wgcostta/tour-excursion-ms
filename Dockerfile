# Dockerfile super simples - sem cache de dependências
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar gradle
RUN apk add --no-cache gradle

# Copiar tudo de uma vez (sem otimização de cache)
COPY . .

# Build direto sem etapas intermediárias
RUN gradle clean bootJar --no-daemon

# Encontrar e renomear JAR
RUN find build/libs -name "*.jar" -exec cp {} app.jar \;

# Stage de produção
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR
COPY --from=builder /app/app.jar app.jar

# Expor porta
EXPOSE 8080

# Executar aplicação
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-Xmx512m", "-jar", "app.jar"]