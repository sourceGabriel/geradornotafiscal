# Gerador de Nota Fiscal

Aplicacao Spring Boot para processamento de pedidos e emissao de nota fiscal com foco em:

- corretude de calculo (subtotal, tributo por item, frete)
- isolamento por requisicao (sem estado compartilhado)
- idempotencia por payload
- resiliencia em integracoes externas simuladas
- observabilidade e qualidade de entrega

## Estado atual da entrega do desafio

- bugs funcionais corrigidos (acumulo entre execucoes e inconsistencias de totais)
- refatoracao arquitetural com `Strategy`, `Resolver`, `Facade` e servicos especializados
- Java 21 + Spring Boot 3.3.6
- testes de regressao e validacao de contrato ampliados
- pipeline CI com `clean verify`
- gate de cobertura JaCoCo em 80%
- ultima cobertura de linhas registrada no relatorio local: **90.36%** (`target/site/jacoco/jacoco.xml`)

## Checklist de aderencia ao desafio

- [x] Nao alterar contrato de entrada (payload mantido com `@JsonProperty`)
- [x] Preservar latencia simulada (`sleep`) das integracoes
- [x] Corrigir acumulacao de estado entre execucoes consecutivas
- [x] Validar consistencia financeira (`valor_total_itens` x subtotal calculado)
- [x] Migrar para Java 21 e Spring Boot 3.3.6
- [x] Cobertura bloqueante no build com JaCoCo >= 80%
- [x] Observabilidade com logs estruturados, MDC e endpoints Actuator
- [x] CI em GitHub Actions com `push` + `pull_request` + `workflow_dispatch`
- [x] Diagramas de arquitetura (fluxo interno + referencia AWS)

## Stack tecnica

- Java 21
- Spring Boot 3.3.6
- Spring Web
- Bean Validation (`jakarta.validation`)
- Spring Actuator
- Micrometer + Prometheus registry
- Spring Retry (`RetryTemplate`)
- Maven Wrapper
- JUnit 5 + Mockito
- JaCoCo
- GitHub Actions

Artefatos gerados:

## Contrato HTTP

### Endpoint

- `POST /api/pedido/gerarNotaFiscal`

### Tipos e enums relevantes

- `tipo_pessoa`: `FISICA` | `JURIDICA`
- `regime_tributacao`: `SIMPLES_NACIONAL` | `LUCRO_REAL` | `LUCRO_PRESUMIDO` | `OUTROS`
- `tipo` (documento): `CPF` | `CNPJ`
- `finalidade` (endereco): `COBRANCA_ENTREGA` | `ENTREGA` | `COBRANCA` | `OUTROS`
- `regiao`: `NORTE` | `NORDESTE` | `CENTRO_OESTE` | `SUDESTE` | `SUL`

### Campos obrigatorios (resumo)

- `id_pedido` > 0
- `data` no formato ISO (`yyyy-MM-dd`)
- pelo menos 1 item em `itens`
- `destinatario.nome` nao vazio
- `destinatario.tipo_pessoa` obrigatorio
- pelo menos 1 endereco em `destinatario.enderecos`
- `destinatario.enderecos[].finalidade` obrigatoria
- `destinatario.enderecos[].regiao` obrigatoria
- `valor_unitario` e `quantidade` dos itens devem ser maiores que zero
- `valor_total_itens` e `valor_frete` nao podem ser negativos

### Payloads de referencia

- `src/main/resources/paylods/teste-pf.json`
- `src/main/resources/paylods/teste-pj-simples.json`

### Exemplo de request

```json
{
  "id_pedido": 1,
  "data": "2022-05-01",
  "valor_total_itens": 100.0,
  "valor_frete": 10.0,
  "itens": [
    {
      "id_item": "1",
      "descricao": "Teclado USB",
      "valor_unitario": 50.0,
      "quantidade": 2
    }
  ],
  "destinatario": {
    "nome": "John Doe",
    "tipo_pessoa": "FISICA",
    "documentos": [
      {
        "tipo": "CPF",
        "numero": "88740347095"
      }
    ],
    "enderecos": [
      {
        "logradouro": "Av do estado",
        "numero": "5533",
        "complemento": "4 andar b",
        "bairro": "Mooca",
        "cidade": "Sao Paulo",
        "estado": "SP",
        "pais": "Brasil",
        "cep": "03105003",
        "finalidade": "ENTREGA",
        "regiao": "SUDESTE"
      }
    ]
  }
}
```

### Exemplo de response de sucesso (200)

```json
{
  "id_nota_fiscal": "uuid",
  "data": "2026-03-30T10:00:00",
  "valor_total_itens": 100.0,
  "valor_frete": 10.48,
  "itens": [
    {
      "id_item": "1",
      "descricao": "Teclado USB",
      "valor_unitario": 50.0,
      "quantidade": 2,
      "valor_tributo_item": 12.0
    }
  ],
  "valor_total_tributos": 12.0,
  "valor_total_nota": 110.48,
  "destinatario": {
    "nome": "John Doe",
    "tipo_pessoa": "FISICA"
  }
}
```

### Erros

Formato padrao (`ApiErrorResponse`):

```json
{
  "timestamp": "2026-03-30T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payload JSON invalido",
  "details": [
    "Campo 'id_pedido' recebeu valor invalido 'abc'. Esperado: Long."
  ]
}
```

Mapeamentos:

- `400 Bad Request`
  - Bean Validation (`MethodArgumentNotValidException`)
  - erro de desserializacao (`HttpMessageNotReadableException`)
  - regra de negocio (`BadRequestException`)
- `502 Bad Gateway`
  - falha em integracao externa (`IntegracaoNotaFiscalException`)
- `500 Internal Server Error`
  - erro nao mapeado

Configuracao de contrato relevante:

- `spring.jackson.deserialization.fail-on-unknown-properties=true`
- campos desconhecidos no JSON retornam `400`

## Regras de negocio

### Calculos

- subtotal = soma de (`valor_unitario * quantidade`) dos itens
- `valor_total_itens` informado deve bater com subtotal calculado
- aliquota resolvida por perfil do destinatario
- tributo por item = (`valor_unitario * quantidade`) * aliquota
- `valor_total_tributos` = soma dos tributos dos itens
- frete ajustado por regiao
- `valor_total_nota = subtotal + valor_frete_ajustado`
- calculo monetario interno com `BigDecimal`, `scale=2`, `RoundingMode.HALF_UP`

> Observacao: `valor_total_nota` nao soma `valor_total_tributos` novamente; os tributos ja sao expostos separadamente no contrato.

### Faixas de aliquota

Pessoa fisica (`tipo_pessoa=FISICA`):

- subtotal < 500 -> 0%
- 500 <= subtotal <= 2000 -> 12%
- 2000 < subtotal <= 3500 -> 15%
- subtotal > 3500 -> 17%

Pessoa juridica (`tipo_pessoa=JURIDICA`):

- `SIMPLES_NACIONAL`: 3%, 7%, 13%, 19% (faixas 1000/2000/5000)
- `LUCRO_REAL`: 3%, 9%, 15%, 20% (faixas 1000/2000/5000)
- `LUCRO_PRESUMIDO`: 3%, 9%, 16%, 20% (faixas 1000/2000/5000)
- sem estrategia compativel: 0%

### Multiplicadores de frete

- `NORTE`: x 1.08
- `NORDESTE`: x 1.085
- `CENTRO_OESTE`: x 1.07
- `SUDESTE`: x 1.048
- `SUL`: x 1.06

## Idempotencia

Implementacao atual: `InMemoryNotaFiscalIdempotencyStore`

- chave gerada por SHA-256 do payload canonico (`PedidoIdempotencyKeyGenerator`)
- deduplicacao concorrente para requisicoes iguais
- estados internos: `IN_PROGRESS`, `COMPLETED`, `FAILED`
- limpeza periodica de expirados (`@Scheduled`)

Configuracoes em `application.properties`:

- `idempotencia.nota-fiscal.ttl-completed-seconds=600`
- `idempotencia.nota-fiscal.ttl-failed-seconds=30`
- `idempotencia.nota-fiscal.cleanup-interval-millis=30000`

## Integracoes simuladas e resiliencia

Orquestracao paralela na `NotaFiscalIntegracaoFacade` usando pool fixo de 4 threads (`AsyncConfig`).

Latencias simuladas atuais:

- `EstoqueService`: `sleep(380ms)`
- `RegistroService`: `sleep(500ms)`
- `FinanceiroService`: `sleep(250ms)`
- `EntregaService`: `sleep(150ms)` + `EntregaIntegrationPort`
- `EntregaIntegrationPort`: `sleep(200ms)`; se `itens > 5`, adiciona `sleep(5000ms)`

Retry de integracoes (backoff exponencial com jitter):

- `integracao.retry.max-attempts=3`
- `integracao.retry.initial-interval-millis=200`
- `integracao.retry.multiplier=2.0`
- `integracao.retry.max-interval-millis=1200`

## Observabilidade

### Logging

- correlation id por request via header `Xitau-Correlation-Id`
- se ausente, o filtro gera UUID
- correlation id propagado para response e MDC
- logs JSON com campos: `timestamp`, `level`, `service`, `correlation_id`, `id_pedido`, `id_nota_fiscal`, `idempotency_key`, entre outros

### Metricas relevantes

- `nota_fiscal.endpoint.duration`
- `nota_fiscal.stage.duration` (validacao, calculo, integracoes, total)
- `nota_fiscal.http.errors` por status (`400`, `502`, `500`)
- `nota_fiscal.idempotency.reuse`
- `nota_fiscal.integracao.duration`
- `nota_fiscal.integracao.success`
- `nota_fiscal.integracao.failure`
- `nota_fiscal.integracao.retry.attempts`
- `nota_fiscal.integracao.success_after_retry`
- `nota_fiscal.integracao.retry.exhausted`
- `nota_fiscal.integracao.consecutive_failures`

Actuator exposto:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

## Testes

Suite atual em `src/test/java/br/com/itau/calculadoratributos` cobre:

- validacao de contrato HTTP e mensagens de erro
- regras de calculo e regressao de totais
- idempotencia (incluindo concorrencia)
- cenario de falha de integracao com retorno `502`
- exposicao de endpoints de observabilidade (`health` e `prometheus`)

## CI

Workflow: `.github/workflows/ci.yml`

- trigger: `push` (`develop`, `main`), `pull_request` para `main`, `workflow_dispatch`
- pipeline principal: `./mvnw -B clean verify`
- upload de artefatos: Surefire + JaCoCo

## Arquitetura

### Desenho arquitetural da solucao e entorno

- Fluxo de system design implementado: `docs/architecture/system-design-mermaid.txt`
- Arquitetura de referencia em nuvem (AWS): `docs/architecture/reference-aws-mermaid.txt`

### Principais componentes e responsabilidades

- `GeradorNFController`: entrada HTTP, validacao de request e metricas de endpoint
- `GeradorNotaFiscalServiceImpl`: orquestracao principal (validacao, subtotal, frete, tributos, idempotencia)
- `FreteCalculator`: regra de frete por regiao com arredondamento monetario
- `TributacaoAliquotaResolver` + strategies: selecao de aliquota por tipo/regime
- `NotaFiscalIntegracaoFacade`: disparo paralelo de integracoes, retries, metricas e logs
- `InMemoryNotaFiscalIdempotencyStore`: deduplicacao por payload e controle de concorrencia
- `ApiExceptionHandler`: contrato padrao de erros (`400`, `502`, `500`)
- `CorrelationIdFilter`: correlacao fim a fim via `Xitau-Correlation-Id` e MDC

### Visao das integracoes, autenticacao, gateway e contexto AWS

- Integracoes externas simuladas: estoque, registro, entrega e financeiro
- Gateway de borda recomendado: API Gateway ou ALB (conforme `reference-aws-mermaid.txt`)
- Autenticacao/autorizacao: proposta de evolucao com Cognito/IdP corporativo (nao implementado no core atual)
- Contexto AWS recomendado para escala:
  - computacao: ECS Fargate (ou EKS)
  - dados: RDS (nota fiscal) e/ou DynamoDB/Redis (idempotencia)
  - mensageria: SQS/SNS para desacoplamento de integracoes
  - operacao: CloudWatch + Prometheus/Grafana + tracing (X-Ray/OpenTelemetry)

## Entrega e operacao

### Estrategia de build e deploy

- Build padrao: `./mvnw -B clean verify`
- Gate de qualidade: JaCoCo minimo de `80%` bloqueando o `verify`
- Empacotamento recomendado: `./mvnw spring-boot:build-image`
- Deploy proposto:
  1. publicar imagem em registry (GHCR/ECR)
  2. promover para homologacao
  3. executar smoke test (`/actuator/health`)
  4. promover para producao com estrategia gradual e rollback por imagem

### Observabilidade, logging e monitoramento (implementado + recomendacoes)

- Implementado:
  - logs JSON via `logback-spring.xml` com MDC (`correlation_id`, `id_pedido`, `id_nota_fiscal`, `idempotency_key`)
  - metricas de etapa, endpoint, erro HTTP e integracoes
  - endpoints Actuator: `health`, `info`, `metrics`, `prometheus`
- Recomendacoes operacionais:
  - alertar aumento de `5xx`/`502` por janela
  - alertar degradacao de latencia `p95/p99`
  - alertar falhas consecutivas por integracao
  - criar dashboards por endpoint e por integracao critica (Grafana/DataDog)

### Pipeline de entrega continua (estado atual e evolucao)

- Estado atual em `.github/workflows/ci.yml`:
  - triggers: `push` (`develop`, `main`), `pull_request` (`main`) e `workflow_dispatch`
  - executa `clean verify`
  - publica artefatos de teste e cobertura
- Evolucao recomendada:
  1. job de build de imagem em branch principal
  2. job de deploy em homologacao
  3. gate manual para producao
  4. job automatizado de rollback e notificacao em falha

## Cobertura de Testes

### [Para visualizar a cobertura de testes clique aqui](https://sourcegabriel.github.io/geradornotafiscal/).

