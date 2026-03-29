# Plano de Fechamento — Desafio Nota Fiscal

## 1) Corretude funcional e confiabilidade

- Garantir isolamento por requisicao no calculo de itens e tributos (sem estado compartilhado).
- Validar payload e regras de negocio antes do processamento.
- Padronizar erros em contrato HTTP:
  - `400` para entrada/regra invalida
  - `502` para falhas de integracao
- Manter idempotencia por requisicao no processamento.

## 2) Arquitetura e manutenibilidade

- Separar regras de tributacao em `Strategy`.
- Usar `Resolver` para selecionar regra por tipo/regime de destinatario.
- Isolar frete em `FreteCalculator`.
- Centralizar orquestracao de integracoes em `Facade`.
- Executar integracoes em paralelo com limite de concorrencia (pool fixo).

## 3) Performance (sem remover latencia simulada)

- Preservar `sleep` de simulacao de integracao.
- Evitar recomputacoes e estruturas compartilhadas entre chamadas.
- Reduzir penalidade extrema para pedidos com muitos itens, mantendo latencia simulada.
- Paralelizar integracoes independentes para reduzir tempo total fim-a-fim.

## 4) Qualidade e testes

- Corrigir testes quebrados e atualizar para Java 21 / Spring Boot 3.
- Cobrir cenarios criticos:
  - regressao de acumulacao entre execucoes
  - consistencia de subtotal/tributos/frete
  - latencia em pedidos com 7+ itens
  - validacao de payload no controller
  - mapeamento de erro de integracao (`502`)
  - matriz de estrategias tributarias

## 5) Modernizacao de stack

- Migrar para Java 21.
- Atualizar para Spring Boot 3.3.x.
- Ajustar plugins Maven (compiler/surefire).
- Incluir Actuator para baseline de operacao.

## 6) Documentacao e operacao

- README com:
  - execucao local e testes
  - explicacao das decisoes tecnicas
  - melhorias implementadas e trade-offs
  - diagrama Mermaid com gateway/auth/integracoes/AWS
  - proposta de CI/CD e observabilidade
- Configuracoes operacionais minimas em `application.properties`.

## 7) Proximos passos recomendados

- Persistencia da nota fiscal + idempotency key.
- Retry com backoff para integracoes externas.
- Contrato de resposta enriquecido com subtotal/tributos/total final explicitos sem quebrar compatibilidade.

