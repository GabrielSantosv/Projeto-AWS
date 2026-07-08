# CONTEXTO PARA CONTINUAÇÃO — Projeto MarketFlow Saga

Você vai continuar o desenvolvimento de um projeto de portfólio backend chamado
**MarketFlow Saga**. Abaixo está TODO o contexto necessário: decisões de
arquitetura já tomadas, convenções obrigatórias, o que já foi implementado
(com código completo) e o que ainda falta fazer. Siga exatamente as decisões
já tomadas — não proponha trocar a arquitetura, apenas continue a partir daqui.

---

## 1. VISÃO GERAL DO PROJETO

Sistema backend (sem frontend) que simula o fluxo de venda de um supermercado
usando **microsserviços orientados a eventos** com **SAGA por coreografia**
(não há orquestrador central controlando o fluxo — cada serviço reage a
eventos do seu domínio e publica novos eventos).

Objetivo: portfólio para GitHub/LinkedIn demonstrando arquitetura backend
distribuída de verdade (não um CRUD comum).

Fluxo de negócio simulado:
1. Caixa cria uma venda (`order-service`)
2. Sistema valida se o funcionário pode operar o caixa (`employee-service`)
3. Estoque reserva os produtos (`inventory-service`)
4. Faturamento aprova a cobrança (`billing-service`)
5. Serviço fiscal tenta emitir a nota fiscal (`fiscal-service`)
6. Se a nota for emitida, o estoque confirma a baixa definitiva (`inventory-service`)
7. Pedido é finalizado (`order-service`)
8. Cliente recebe notificação (`notification-service`)
9. Se estoque ficar baixo, `supplier-service` reage e cria pedido de reposição
10. `saga-tracker-service` observa TODOS os eventos (sem agir) e monta a
    timeline de cada venda — é o diferencial de observabilidade do projeto

---

## 2. DECISÕES ARQUITETURAIS OBRIGATÓRIAS (não mudar)

- **SAGA por coreografia**, não por orquestração. Nenhum serviço manda em
  outro; cada um reage a eventos e publica os seus.
- **Banco de dados por serviço** (database-per-service). Nunca compartilhar
  banco entre serviços. Cada serviço é dono exclusivo dos seus dados.
- **Comunicação assíncrona via eventos** (SNS/SQS) no fluxo principal da
  venda. Chamadas REST diretas entre serviços SÓ são aceitáveis para leitura
  administrativa/CRUD fora do fluxo da saga (ex: consultar produto).
- **Floci** no lugar do LocalStack para emular AWS localmente (SNS/SQS).
  Floci é open-source, MIT, drop-in replacement do LocalStack — mesma porta
  4566, mesmo protocolo AWS, imagem Docker `floci/floci:latest`. Motivo da
  troca: LocalStack fechou a Community Edition e passou a exigir auth token.
- **Estoque nunca é baixado direto.** Fluxo obrigatório: reservar estoque →
  aprovar faturamento → emitir nota fiscal → SÓ ENTÃO confirmar baixa
  definitiva. Se qualquer etapa depois da reserva falhar, a reserva é liberada
  (compensação).
- **Idempotência obrigatória em todo consumidor de evento.** SQS entrega
  "at-least-once", então mensagens duplicadas VÃO acontecer. Cada serviço tem
  uma tabela `processed_events` (chave = `eventId`) e checa antes de agir.
- **DLQ (Dead Letter Queue) por fila**, com `maxReceiveCount` configurado.
  Mensagem que falha N vezes vai pra DLQ e não trava o restante do sistema.
- **Envelope de evento padrão** (todo evento usa essa estrutura):
  ```json
  {
    "eventId": "uuid",
    "eventType": "StockReserved",
    "sagaId": "orderId (mesmo valor do pedido)",
    "correlationId": "uuid da conversa/requisição original",
    "timestamp": "ISO-8601",
    "version": 1,
    "payload": { }
  }
  ```
- **Topologia de mensageria**: 1 tópico SNS único (`saga-events-topic`) +
  1 fila SQS por serviço, cada fila com **filter policy** no atributo
  `eventType` da mensagem SNS, para receber só os eventos que interessam ao
  seu domínio. Cada fila tem sua DLQ correspondente.
- **Contrato de evento enriquecido**: eventos carregam os dados que o próximo
  consumidor precisa, mesmo que isso duplique informação. Exemplo real já
  aplicado: `EmployeeValidated` inclui a lista de itens do pedido (copiada de
  `OrderCreated`), assim o `inventory-service` reserva estoque sem precisar
  consumir `OrderCreated` separadamente. Sempre que criar um novo evento,
  pense em "o que o próximo consumidor precisa pra agir sozinho" e inclua no
  payload.

### Catálogo de eventos

Publicados/consumidos por serviço (quem publica → quem consome):

| Evento | Publicado por | Consumido por |
|---|---|---|
| OrderCreated | order-service | employee-service |
| EmployeeValidated | employee-service | order-service, inventory-service |
| EmployeeValidationFailed | employee-service | order-service |
| StockReserved | inventory-service | order-service, billing-service |
| StockReservationFailed | inventory-service | order-service |
| BillingApproved | billing-service | order-service, fiscal-service |
| BillingRejected | billing-service | order-service, inventory-service |
| InvoiceIssued | fiscal-service | order-service, inventory-service |
| InvoiceRejected | fiscal-service | order-service, inventory-service |
| StockConfirmed | inventory-service | order-service |
| StockReservationReleased | inventory-service | order-service |
| OrderCompleted | order-service | notification-service, saga-tracker-service |
| OrderCanceled | order-service | notification-service, saga-tracker-service |
| NotificationSent / NotificationFailed | notification-service | saga-tracker-service |
| StockLowDetected | inventory-service | supplier-service |
| PurchaseOrderCreated | supplier-service | saga-tracker-service |

`saga-tracker-service` consome **todos** os eventos (fila sem filtro).

### Status possíveis do pedido (order-service)
`CREATED`, `EMPLOYEE_VALIDATED`, `STOCK_RESERVED`, `BILLING_APPROVED`,
`WAITING_INVOICE`, `INVOICE_ISSUED`, `COMPLETED`, `CANCELED`, `FAILED`

### Bancos por serviço
`order_db`, `employee_db`, `inventory_db`, `billing_db`, `fiscal_db`,
`notification_db`, `supplier_db`, `saga_tracker_db`

### Stack técnica
Java 21, Spring Boot 3.3.2, Spring Data JPA, PostgreSQL, AWS SDK v2
(`software.amazon.awssdk:sqs` e `:sns`, versão 2.26.12) apontando pro Floci,
Lombok, springdoc-openapi 2.6.0, Docker/Docker Compose, JUnit, Testcontainers
(fase 3).

---

## 3. O QUE JÁ FOI IMPLEMENTADO: `inventory-service` (parcialmente completo)

Localização: `services/inventory-service/`

### Arquivos já criados (código completo, pronto pra rodar após completar o que falta):

```
services/inventory-service/
├── pom.xml                                          [completo]
└── src/main/java/com/marketflow/inventory/
    ├── InventoryServiceApplication.java             [completo]
    ├── config/
    │   └── AwsConfig.java                            [completo — SqsClient/SnsClient apontando pra Floci via endpointOverride]
    ├── domain/
    │   ├── Product.java                              [completo — quantityOnHand, quantityReserved, quantityAvailable calculado]
    │   ├── StockReservation.java                     [completo — 1 linha por orderId+productId, status RESERVED/CONFIRMED/RELEASED]
    │   ├── ReservationStatus.java                     [completo — enum]
    │   └── ProcessedEvent.java                        [completo — tabela de idempotência]
    ├── repository/
    │   ├── ProductRepository.java                     [completo — findByIdForUpdate com PESSIMISTIC_WRITE lock]
    │   ├── StockReservationRepository.java             [completo]
    │   └── ProcessedEventRepository.java               [completo]
    ├── event/
    │   ├── EventEnvelope.java                          [completo]
    │   ├── EventType.java                              [completo — constantes de todos os eventTypes]
    │   ├── dto/
    │   │   ├── OrderItem.java                          [completo]
    │   │   ├── EmployeeValidatedPayload.java           [completo — inclui items]
    │   │   ├── InvoiceIssuedPayload.java               [completo]
    │   │   ├── InvoiceRejectedPayload.java             [completo]
    │   │   ├── BillingRejectedPayload.java             [completo]
    │   │   ├── StockReservedPayload.java               [completo]
    │   │   ├── StockReservationFailedPayload.java      [completo]
    │   │   ├── StockConfirmedPayload.java              [completo]
    │   │   ├── StockReservationReleasedPayload.java    [completo]
    │   │   └── StockLowDetectedPayload.java            [completo]
    │   ├── publisher/
    │   │   └── EventPublisher.java                     [completo — publica no SNS com atributo eventType]
    │   └── listener/
    │       └── SagaEventListener.java                  [completo — poller SQS @Scheduled, checa idempotência, roteia por eventType, deleta mensagem só se processar com sucesso]
    ├── service/
    │   └── InventoryService.java                       [completo — reserveStock, confirmStock, releaseReservation, adjustStock (restock/write-off manual), checkLowStock]
    ├── exception/
    │   └── ProductNotFoundException.java               [completo]
    └── controller/
        └── ProductDtos.java                            [completo — CreateProductRequest, AdjustStockRequest, ProductResponse]
```

### O que a lógica de negócio do `InventoryService` já faz:

- `reserveStock(orderId, correlationId, items)`: reserva cada item com lock
  pessimista; se QUALQUER item falhar (produto não existe ou estoque
  insuficiente), desfaz tudo que já reservou pra esse pedido e publica
  `StockReservationFailed`; se tudo der certo, publica `StockReserved` e
  dispara `checkLowStock` por produto.
- `confirmStock(orderId, correlationId)`: transforma reservas `RESERVED` em
  `CONFIRMED` e SÓ AQUI deduz `quantityOnHand` de verdade. Publica
  `StockConfirmed`. Idempotente (pula reservas que não estão mais `RESERVED`).
- `releaseReservation(orderId, correlationId, reason)`: libera reservas
  (`RESERVED` → `RELEASED`), devolve a quantidade reservada pro estoque
  disponível. Publica `StockReservationReleased`.
- `adjustStock(productId, delta)`: ajuste manual direto (repor de fornecedor
  ou baixa manual por perda/avaria) — NÃO faz parte do fluxo da saga, é usado
  pelo endpoint REST administrativo. Bloqueia se o delta deixaria
  `quantityOnHand` menor que `quantityReserved`.
- `checkLowStock(product)`: se `quantityAvailable <= lowStockThreshold`,
  publica `StockLowDetected`.

---

## 4. O QUE FALTA FAZER NO `inventory-service` (próximos passos imediatos)

1. **`ProductController.java`** — endpoints REST administrativos:
   - `POST /products` (criar produto, usa `CreateProductRequest`)
   - `GET /products/{id}` (retorna `ProductResponse`)
   - `GET /products` (listar todos)
   - `POST /products/{id}/stock-adjustments` (usa `AdjustStockRequest`, chama
     `inventoryService.adjustStock`)
   - Adicionar mapper entre `Product` (entidade) e `ProductResponse` (DTO)

2. **`application.yml`** — configuração do Spring Boot:
   - datasource PostgreSQL (`inventory_db`)
   - `aws.endpoint=http://floci:4566` (ou `http://localhost:4566` fora do Docker)
   - `aws.region=us-east-1`
   - `aws.sns.saga-topic-arn` (definido depois que o script do Floci criar o tópico)
   - `aws.sqs.inventory-queue-url` (idem, criado pelo script de init)
   - `aws.sqs.poll-interval-ms=2000`
   - JPA: `ddl-auto=validate` (usar migration, ver item 3) ou `update` só em
     dev

3. **Migrations** (Flyway ou Liquibase — decidir e adicionar dependência no
   pom.xml) para criar as tabelas `products`, `stock_reservations`,
   `processed_events` no `inventory_db`. Não depender de `ddl-auto=update`
   em portfólio sério — Flyway é mais profissional pro README.

4. **`Dockerfile`** do `inventory-service` (multi-stage build: Maven build →
   JRE slim runtime).

5. **`init-aws.sh`** (script do Floci, em `infra/floci/`) — precisa criar:
   - tópico SNS `saga-events-topic`
   - fila SQS `inventory-service-queue` + `inventory-service-dlq`
   - assinatura da fila no tópico com filter policy:
     `{"eventType": ["EmployeeValidated", "InvoiceIssued", "InvoiceRejected", "BillingRejected"]}`
   - redrive policy apontando pra DLQ com `maxReceiveCount` (ex: 5)
   - Repetir esse padrão pra cada serviço quando forem criados

6. **Testes** (JUnit + Mockito no mínimo para a Fase 1; Testcontainers com
   Floci na Fase 3):
   - `reserveStock` com sucesso
   - `reserveStock` com estoque insuficiente (rollback correto)
   - `reserveStock` com produto inexistente
   - `confirmStock` idempotente (chamar duas vezes não deduz duas vezes)
   - `releaseReservation` devolve quantidade corretamente
   - Listener ignora evento já processado (idempotência)

---

## 5. PRÓXIMOS MICROSSERVIÇOS (na ordem sugerida)

Depois do `inventory-service` estar completo e testável isoladamente:

1. **`order-service`** — cria o pedido, mantém status, reage a praticamente
   todos os eventos de sucesso/falha pra atualizar o status e decidir quando
   publicar `OrderCompleted`/`OrderCanceled`. É o serviço mais "central" em
   termos de lógica de estado (state machine do pedido).

2. **`employee-service`** — o mais simples. Consome `OrderCreated`, valida
   funcionário, publica `EmployeeValidated` (lembrando de enriquecer com
   `items`, copiados do payload de `OrderCreated`) ou
   `EmployeeValidationFailed`.

3. **`billing-service`** — consome `StockReserved`, simula aprovação/rejeição
   de cobrança, publica `BillingApproved`/`BillingRejected`.

4. **`fiscal-service`** — consome `BillingApproved`, simula emissão de nota
   fiscal (incluir cenário de falha por CPF irregular/cancelado), publica
   `InvoiceIssued`/`InvoiceRejected`.

5. **`notification-service`** — consome `OrderCompleted`/`OrderCanceled`,
   simula envio de notificação, publica `NotificationSent`/`NotificationFailed`.

6. **`supplier-service`** — consome `StockLowDetected`, cria pedido de
   reposição fictício, publica `PurchaseOrderCreated`.

7. **`saga-tracker-service`** — fila SEM filtro (recebe todos os eventos).
   Grava cada evento recebido associado ao `sagaId`. Expõe:
   - `GET /sagas/{orderId}` → timeline completa de eventos daquele pedido
   - `GET /sagas?status=CANCELED` → lista de sagas por status
   Importante: ele SÓ observa e persiste, nunca publica eventos de negócio
   nem decide nada pelo fluxo.

8. **`api-gateway`** — recebe requisições externas (`POST /sales`,
   `GET /sales/{id}`, `GET /sales/{id}/status`) e encaminha pro
   `order-service`. Sem regra de negócio.

Cada serviço novo deve seguir exatamente o MESMO padrão estrutural já usado
no `inventory-service`: `config/AwsConfig`, `domain/`, `repository/`,
`event/EventEnvelope` + `EventType` + `dto/` + `publisher/EventPublisher` +
`listener/SagaEventListener` (adaptando o roteamento pro que aquele serviço
consome), `service/`, `exception/`, `controller/`.

---

## 6. INFRAESTRUTURA GERAL (ainda não criada)

- `docker-compose.yml` na raiz: Floci + 1 container Postgres por serviço (ou
  1 Postgres com múltiplos databases) + 1 container por microsserviço.
- `infra/floci/init-aws.sh`: cria TODOS os tópicos/filas/DLQs/filter
  policies de uma vez, pra subir o ambiente inteiro com `docker-compose up`.
- `infra/postman/marketflow-saga.postman_collection.json`: coleção com os
  endpoints administrativos de cada serviço + o endpoint de criação de venda
  no gateway.
- `docs/`: `architecture.md`, `events-catalog.md`, `saga-flows.md`
  (sucesso + cenários de falha), `failure-scenarios.md`, `linkedin-post.md`,
  diagramas.
- `shared/`: se decidir extrair o `EventEnvelope` e os DTOs de evento pra um
  módulo Maven compartilhado entre serviços (evita duplicar `EventEnvelope`
  em cada serviço). Avaliar se compensa a complexidade extra de um projeto
  multi-módulo Maven pra esse estágio do portfólio.

## 7. README e material de divulgação (fase final)

Estrutura do README já definida (ver seção anterior da conversa): Overview,
Architecture, Why Choreography over Orchestration, Services, Event Catalog,
Saga Success Flow, Saga Failure Scenarios, Idempotency Strategy, Retry & DLQ
Strategy, Saga Tracker, Tech Stack, How to Run, API Docs, Postman Collection,
Testing Strategy, Future Improvements.

Texto de post pro LinkedIn já rascunhado — reaproveitar e ajustar conforme o
projeto evoluir.

---

## 8. PONTOS DE ATENÇÃO / RISCOS JÁ IDENTIFICADOS (não esquecer)

- Ordem de entrega de eventos entre filas diferentes NÃO é garantida — cada
  serviço deve tratar isso via máquina de estados explícita, nunca assumir
  ordem implícita.
- `saga-tracker-service` nunca deve virar orquestrador — só observa e persiste.
- DLQ sem processo de reprocessamento documentado é inútil — definir pelo
  menos um script/processo manual de "replay" de mensagens da DLQ.
- Lock pessimista (`findByIdForUpdate`) no `inventory-service` evita
  overselling em reservas concorrentes — manter esse padrão se replicar
  lógica parecida em outros serviços com concorrência (ex: billing).
