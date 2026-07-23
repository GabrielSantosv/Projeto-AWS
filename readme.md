PROJETO SAGA AWS

Este repositório é um projeto de estudo e demonstração de uma arquitetura baseada em microsserviços com comunicação assíncrona via eventos. A ideia é simular um fluxo de vendas de supermercado, onde uma compra gera uma cadeia de ações entre diferentes serviços, com compensação em caso de falha.

## Proposta do projeto

O MarketFlow mostra, na prática, como funciona uma arquitetura orientada a eventos com padrão SAGA, onde:

- um pedido é criado no serviço de pedidos;
- o estoque é reservado em um serviço dedicado;
- a nota fiscal é emitida em outro serviço;
- em caso de problema, ações compensatórias são executadas para evitar inconsistências.

A proposta principal é demonstrar conceitos como:

- microsserviços independentes;
- comunicação via SNS/SQS;
- outbox pattern;
- idempotência no processamento de eventos;
- compensação de falhas em fluxo distribuído.

---

## Arquitetura geral

O projeto foi pensado em camadas de domínio e comunicação:

1. Pedido é criado.
2. Um evento de pedido é publicado.
3. Os serviços interessados consomem esse evento.
4. Cada serviço executa sua regra de negócio e publica novos eventos, se necessário.
5. Se alguma etapa falhar, a saga é compensada.

Em termos práticos, o fluxo de exemplo é:

- criar pedido;
- confirmar pagamento;
- reservar estoque;
- emitir nota fiscal;
- em caso de estoque insuficiente, cancelar a operação e compensar o que já foi feito.

---

## Tecnologias utilizadas

- Java 21
- Spring Boot 3.3.2
- Spring Data JPA
- PostgreSQL
- Flyway
- AWS SDK v2 para SNS/SQS
- LocalStack/Floci para simular AWS localmente
- JUnit 5 e Mockito

---

## Estrutura do repositório

- infra/floci: scripts para preparar o ambiente local com SNS, SQS e filas.
- services/estoque-service: serviço de estoque com API em português.
- services/inventory-service: serviço de estoque com implementação complementar em inglês.
- services/pedido-service: serviço responsável pelo ciclo de vida dos pedidos.
- services/fiscal-service: serviço responsável por emissão e cancelamento de notas fiscais.

---

## Como executar localmente

### 1. Pré-requisitos

- Java 21
- Maven
- Docker
- PostgreSQL rodando localmente
- Ambiente AWS local com Floci/LocalStack

### 2. Preparar o ambiente local

No diretório raiz:

```bash
cd infra/floci
./init-aws.sh
```

### 3. Rodar os serviços

Exemplo para o serviço de pedido:

```bash
cd services/pedido-service
mvn spring-boot:run
```

Os demais serviços seguem a mesma lógica:

```bash
cd services/estoque-service
mvn spring-boot:run
```

```bash
cd services/inventory-service
mvn spring-boot:run
```

```bash
cd services/fiscal-service
mvn spring-boot:run
```

Os serviços usam configurações padrão com bancos separados, como:

- pedido_db
- estoque_db
- inventory_db
- fiscal_db

E o endpoint local da simulação AWS em:

- http://localhost:4566

---

## Como cada microserviço funciona

### 1. pedido-service

Responsável pelo ciclo de vida do pedido.

Funções principais:

- criar pedido;
- confirmar pagamento;
- publicar eventos da saga;
- processar compensações quando o estoque é insuficiente;
- manter o estado do pedido, como aguardando pagamento, pago, processando e cancelado.

Esse serviço é o ponto de entrada do fluxo de negócio.

### Endpoints principais

- POST /pedidos
- POST /pedidos/{id}/confirmar-pagamento
- GET /pedidos/{id}

### 2. estoque-service

Responsável pelo controle de produtos e estoque.

Funções principais:

- criar produtos;
- consultar produtos;
- ajustar estoque;
- controlar reservas e disponibilidade.

Esse serviço representa a base operacional do inventário.

### Endpoints principais

- POST /produtos
- GET /produtos
- GET /produtos/{produtoId}
- POST /produtos/{produtoId}/estoque-ajustes

### 3. inventory-service

É uma implementação complementar do mesmo domínio de estoque, com estrutura mais próxima de um modelo de referência em inglês.

Funções principais:

- cadastro de produtos;
- ajustes de estoque;
- consulta de saldo e reservas.

Ele serve como uma segunda visão do mesmo problema, útil para comparar abordagens e evoluir a solução.

### Endpoints principais

- POST /products
- GET /products
- GET /products/{id}
- POST /products/{id}/stock-adjustments

### 4. fiscal-service

Responsável por emitir e cancelar notas fiscais.

Funções principais:

- gerar nota fiscal quando o pedido é criado com sucesso;
- cancelar a nota se o estoque for insuficiente;
- registrar eventos já processados para garantir idempotência.

Esse serviço mostra como uma etapa downstream pode reagir a eventos de negócio sem depender diretamente do pedido-service.

### Endpoints principais

- GET /notas-fiscais
- GET /notas-fiscais/{pedidoId}

---

## Fluxo de negócio em exemplo

1. Um cliente cria um pedido no pedido-service.
2. O pedido publica um evento de criação.
3. O serviço de estoque reage e reserva a quantidade solicitada.
4. O serviço fiscal reage e emite a nota fiscal.
5. Se faltar estoque, um evento de compensação é disparado.
6. O pedido é cancelado e a nota fiscal, se existir, é cancelada.

Esse comportamento representa uma saga de compensação em execução.

---

## Status atual

O projeto já possui a base estrutural e funcional para demonstrar o fluxo principal com eventos, armazenamento local e testes unitários. A intenção é servir como referência para quem quer estudar:

- microsserviços com Spring Boot;
- comunicação assíncrona;
- padrões de saga e outbox;
- integração com serviços de fila e tópicos.

---

## Próximos passos sugeridos

- conectar mais serviços na mesma saga;
- expandir o fluxo para notificações e expedição;
- validar o comportamento com integração real contra SNS/SQS;
- adicionar testes de integração mais completos.
