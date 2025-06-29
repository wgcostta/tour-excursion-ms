# Tour Application 🌍

Uma aplicação Spring Boot para gerenciamento de tours e viagens organizadas.

## 📋 Características

- **API RESTful** completa para gestão de tours
- **Documentação automática** com Swagger/OpenAPI
- **Validação robusta** de dados de entrada
- **Tratamento global** de exceções
- **Testes unitários e de integração** abrangentes
- **Containerização** com Docker
- **Configuração multi-ambiente** (dev, test, prod)

## 🚀 Tecnologias

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring Web**
- **H2 Database** (desenvolvimento)
- **PostgreSQL** (produção)
- **Gradle** (build)
- **Docker**
- **JUnit 5** (testes)
- **Swagger/OpenAPI 3** (documentação)

## 📦 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/tourapp/
│   │   ├── controller/     # Controllers REST
│   │   ├── service/        # Lógica de negócio
│   │   ├── repository/     # Camada de dados
│   │   ├── entity/         # Entidades JPA
│   │   ├── dto/           # Data Transfer Objects
│   │   ├── exception/     # Tratamento de exceções
│   │   └── TourApplication.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/tourapp/
        ├── service/       # Testes unitários
        └── controller/    # Testes de integração
```

## 🛠️ Executando Localmente

### Pré-requisitos

- Java 17+
- Docker (opcional)

### Usando Gradle

```bash
# Clone o repositório
git clone <repository-url>
cd tour-application

# Execute a aplicação
./gradlew bootRun

# Ou compile e execute o JAR
./gradlew build
java -jar build/libs/tour-application-1.0.0-boot.jar
```

### Usando Docker

```bash
# Build da imagem
docker build -t tour-app .

# Execute o container
docker run -p 8080:8080 tour-app
```

## 📖 API Endpoints

### Tours

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/tours` | Criar novo tour |
| `GET` | `/api/v1/tours` | Listar todos os tours (paginado) |
| `GET` | `/api/v1/tours/{id}` | Buscar tour por ID |
| `PUT` | `/api/v1/tours/{id}` | Atualizar tour |
| `DELETE` | `/api/v1/tours/{id}` | Deletar tour |
| `GET` | `/api/v1/tours/search` | Buscar tours com filtros |
| `GET` | `/api/v1/tours/status/{status}` | Buscar tours por status |
| `GET` | `/api/v1/tours/destinations/popular` | Destinos populares |

### Exemplo de Payload

```json
{
  "name": "Amazing Paris Tour",
  "description": "Explore the beautiful city of Paris with expert guides",
  "destination": "Paris, France",
  "price": 1299.99,
  "durationDays": 7,
  "maxParticipants": 20,
  "status": "ACTIVE",
  "imageUrl": "https://example.com/paris.jpg"
}
```

## 📝 Documentação da API

Após iniciar a aplicação, acesse:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

## 🗄️ Banco de Dados

### Desenvolvimento (H2)
- **Console H2**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:tourdb`
- **Usuário**: `sa`
- **Senha**: `password`

### Produção (PostgreSQL)
Configure as variáveis de ambiente:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

## 🧪 Testes

```bash
# Executar todos os testes
./gradlew test

# Executar apenas testes unitários
./gradlew test --tests "*Test"

# Executar apenas testes de integração
./gradlew test --tests "*IntegrationTest"

# Relatório de cobertura
./gradlew jacocoTestReport
```

## 🔍 Monitoramento

A aplicação inclui endpoints do Spring Boot Actuator:

- **Health Check**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Metrics**: http://localhost:8080/actuator/metrics

## 📊 Funcionalidades Principais

### Entidade Tour

- **ID único** (UUID)
- **Nome** (3-100 caracteres)
- **Descrição** (10-1000 caracteres)
- **Destino** (2-100 caracteres)
- **Preço** (valor decimal positivo)
- **Duração** (1-365 dias)
- **Máximo de participantes** (1-100)
- **Status** (ACTIVE, INACTIVE, CANCELLED, FULL)
- **URL da imagem** (opcional)
- **Timestamps** de criação e atualização

### Validações

- Validação de dados de entrada com Bean Validation
- Verificação de duplicatas por nome
- Tratamento de exceções personalizado
- Respostas de erro estruturadas

### Busca e Filtros

- **Paginação** e ordenação
- **Filtros múltiplos**: nome, destino, preço, duração, status
- **Busca por status** específico
- **Destinos populares** baseados na quantidade de tours

## 🔧 Configuração por Ambiente

### Desenvolvimento (`dev`)
- Banco H2 em memória
- Logs detalhados
- Console H2 habilitado

### Teste (`test`)
- Banco H2 separado para testes
- Logs mínimos
- Dados limpos a cada teste

### Produção (`prod`)
- PostgreSQL
- Logs para arquivo
- Configurações de segurança

## 🚀 Deploy

### Docker Compose (Exemplo)

```yaml
version: '3.8'
services:
  tour-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/tourdb
      - DATABASE_USERNAME=touruser
      - DATABASE_PASSWORD=tourpass
    depends_on:
      - db