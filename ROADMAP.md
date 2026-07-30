# Roadmap para o Engine Lite 1.0.0

Este roadmap aplica os gates aprovados na
[Issue #9](https://github.com/Balehlah/Engine_lite/issues/9). O índice completo
do programa permanece na
[Issue #1](https://github.com/Balehlah/Engine_lite/issues/1).

## Princípios de execução

1. O produto é o motor; demos validam o motor, mas não definem regras de gameplay
   no core.
2. Tarefas só começam quando todas as dependências declaradas estiverem
   fechadas.
3. Mudanças de arquitetura exigem ADR ou aprovação explícita do
   `technical-coordinator`.
4. Evidência do `qa_validator` precede a aprovação de gates técnicos.
5. Nenhuma decisão aceita é reescrita; uma mudança cria nova ADR.

## P1 — Fundação

| Ordem | Entrega | Gate de entrada | Gate de saída |
|---|---|---|---|
| 1 | [#9 — Decisões e ADRs](https://github.com/Balehlah/Engine_lite/issues/9) | Aprovação explícita do mantenedor | Decisões rastreáveis, ADR-002 mensurável, política da 1.0.0 e pacotes definidos |
| 2 | [#10 — Gradle, toolchain e módulos](https://github.com/Balehlah/Engine_lite/issues/10) | #9 fechada | Java 21, módulos e build reproduzível; smoke de compatibilidade em Java 25 |
| 3 | [#11 — Baseline e regressões](https://github.com/Balehlah/Engine_lite/issues/11) | Contrato de build suficiente | Comportamento do protótipo capturado por testes |
| 4 | [#12 — CI multiplataforma](https://github.com/Balehlah/Engine_lite/issues/12) | Build reproduzível | Baseline histórica concluída; contrato de plataforma substituído pela D-010 |
| 5 | [#13 — Licença, SemVer e API](https://github.com/Balehlah/Engine_lite/issues/13) | #9 e #10 fechadas | Apache-2.0 aplicado, inventários presentes e baseline de API funcionando |
| 6 | [#14 — Spike libGDX/LWJGL3](https://github.com/Balehlah/Engine_lite/issues/14) | #10, #12 e #13 fechadas; #60 integrada e validada | ADR-002/D-011 aceita libGDX/LWJGL3 com matriz Windows/Linux completa e Java2D preservado |

## Contratos que desbloqueiam o restante

- Runtime independente de backend só pode depender de contratos do módulo core.
- A API estável só existe após a baseline da Issue #13.
- Trabalho dependente de GPU pode prosseguir após a conclusão formal da #14,
  conforme a decisão ADR-002/D-011 e os boundaries vigentes.
- O viewport de referência é 320×180 até a Issue #22 concluir seus testes.
- Packaging e release devem cobrir Windows e Linux, conforme D-010/ADR-006.

## Release 1.0.0

A versão 1.0.0 futura é a primeira release pública suportada sob SemVer. Ela não
é continuação contratual dos marcos `prototype-1.0.0` e `prototype-2.0.0` de
2025. O freeze da API ocorre somente no primeiro release candidate, conforme
[docs/versioning.md](docs/versioning.md).

## Revisão

Revisar este roadmap quando:

- uma dependência mudar de contrato;
- uma ADR for substituída;
- o spike de backend terminar;
- o primeiro RC for iniciado;
- uma família de sistema operacional deixar de ser tecnicamente suportável.
