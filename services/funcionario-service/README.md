# funcionario-service

Servico REST sincrono de autenticacao de operadores. Ele nao participa da coreografia MarketFlow e nao possui AWS SDK, SQS, SNS ou pasta `event`.

## Token e contrato

O login retorna um token opaco UUID; apenas seu hash SHA-256 e persistido em `sessoes_caixa`. Nao e JWT. Cada token nasce com 60 minutos de validade por padrao (configuravel por `AUTH_SESSION_DURATION_MINUTES`). O token nao carrega dados nem deve ser interpretado por outro servico.

`POST /auth/login`

```json
{ "matricula": "OP-001", "senha": "segredo" }
```

Resposta `200`:

```json
{ "token": "uuid-opaco", "tipo": "Bearer", "funcionarioId": "uuid", "matricula": "OP-001", "nome": "Nome", "cargo": "OPERADOR", "expiraEm": "2026-07-28T20:00:00Z" }
```

`GET /auth/validar`

Aceita `Authorization: Bearer <token>` (ou `?token=<token>` para chamadas internas simples). Responde `200` mesmo quando invalido, com `valido: false`; isso permite que o `pedido-service` decida sua propria resposta HTTP.

Resposta valida:

```json
{ "valido": true, "funcionarioId": "uuid", "matricula": "OP-001", "nome": "Nome", "cargo": "OPERADOR", "expiraEm": "2026-07-28T20:00:00Z" }
```

Resposta invalida ou expirada:

```json
{ "valido": false, "funcionarioId": null, "matricula": null, "nome": null, "cargo": null, "expiraEm": null }
```

Somente funcionarios ativos com cargo listado em `AUTH_CARGOS_AUTORIZADOS` (por padrao `OPERADOR`, `SUPERVISOR` e `GERENTE`) podem iniciar sessao.
