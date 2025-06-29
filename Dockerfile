# Dockerfile com New Relic Agent - Versão Mais Robusta
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar dependências e verificar conectividade
RUN apk add --no-cache gradle wget unzip ca-certificates \
    && update-ca-certificates

# Verificar conectividade e baixar New Relic Agent
RUN echo "Testando conectividade..." \
    && wget --spider https://download.newrelic.com/ \
    && echo "Baixando New Relic Agent..." \
    && wget --timeout=30 --tries=3 -O newrelic-java.zip \
       https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && echo "Extraindo New Relic Agent..." \
    && unzip -q newrelic-java.zip \
    && rm newrelic-java.zip \
    && echo "Verificando conteúdo extraído:" \
    && ls -la newrelic/ \
    && test -f newrelic/newrelic.jar \
    && echo "New Relic Agent instalado com sucesso!"

# Copiar código da aplicação
COPY . .

# Build da aplicação
RUN gradle clean bootJar --no-daemon

# Encontrar e renomear JAR
RUN find build/libs -name "*.jar" -exec cp {} app.jar \;

# Stage de produção
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instalar dependências mínimas para produção
RUN apk add --no-cache wget ca-certificates \
    && update-ca-certificates

# Copiar JAR da aplicação
COPY --from=builder /app/app.jar app.jar

# Copiar New Relic Agent
COPY --from=builder /app/newrelic/ ./newrelic/

# Verificar se o agente foi copiado corretamente
RUN test -f ./newrelic/newrelic.jar || (echo "Erro: newrelic.jar não encontrado!" && exit 1)

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
ENTRYPOINT ["sh", "-c", "echo 'Iniciando aplicação com New Relic Agent...' && java $JAVA_OPTS -javaagent:./newrelic/newrelic.jar -Dserver.port=${PORT:-8080} -jar app.jar"]