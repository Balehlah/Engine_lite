# Registro de decisões de arquitetura

Este arquivo é o índice normativo das decisões de fundação do Engine Lite.
Detalhes, alternativas, consequências e gatilhos de revisão ficam nos ADRs
vinculados.

## Governança

- Aprovador e responsável: [@Balehlah](https://github.com/Balehlah), no papel
  de `technical-coordinator`.
- Data da decisão: 2026-07-24.
- Evidência de aprovação: o mantenedor aprovou explicitamente o pacote completo
  de nove decisões durante a execução da Issue #9.
- Alteração de uma decisão aceita: somente por nova ADR, sem apagar ou
  reescrever a decisão anterior.
- Validação de gates técnicos: papel `qa_validator`, antes da aprovação final
  pelo `technical-coordinator`.

## Registro mestre

| ID | Escolha | Responsável | Data | Justificativa | Consequência principal | Gatilho de revisão | ADR | Issue | Gate |
|---|---|---|---|---|---|---|---|---|---|
| D-001 | Engine Lite é o produto; `game.test` é demo consumidora | @Balehlah (`technical-coordinator`) | 2026-07-24 | O backlog e a separação `engine`/`game` têm como objetivo um motor reutilizável | Sistemas específicos de um jogo não entram no core | Mudança formal da visão do produto | [ADR-001](docs/adr/ADR-001-product-platform-runtime.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Escopo das PRs mantém demo separada do motor |
| D-002 | Suporte desktop a Windows, Linux e macOS | @Balehlah (`technical-coordinator`) | 2026-07-24 | Distribuição desktop multiplataforma é requisito do programa 1.0.0 | Build, smoke e packaging devem cobrir as três famílias | Plataforma sem suporte do JDK/backend ou nova plataforma solicitada | [ADR-001](docs/adr/ADR-001-product-platform-runtime.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | CI verde nas três famílias antes do backend/release |
| D-003 | Java 21 LTS como baseline; Java 25 LTS como compatibilidade | @Balehlah (`technical-coordinator`) | 2026-07-24 | Java 21 equilibra LTS e compatibilidade; Java 25 antecipa a LTS seguinte sem elevar o bytecode mínimo | Código principal compila para 21 e recebe smoke adicional em 25 | Dependência exigir versão superior ou revisão da próxima major | [ADR-001](docs/adr/ADR-001-product-platform-runtime.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Toolchain fixa na #10 e matriz validada na #12 |
| D-004 | Código sob Apache-2.0; assets somente com origem e licença compatível | @Balehlah (`technical-coordinator`) | 2026-07-24 | Permite uso amplo com concessão explícita de patentes e rastreabilidade | `LICENSE`, notices e inventário serão obrigatórios | Incompatibilidade identificada ou mudança do modelo de distribuição | [ADR-004](docs/adr/ADR-004-license-and-assets.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Aplicação e relatório de licenças na #13 |
| D-005 | A futura 1.0.0 será a primeira release com contrato SemVer público | @Balehlah (`technical-coordinator`) | 2026-07-24 | As versões históricas do changelog não possuem tags nem baseline auditada | Marcos 2025 permanecem identificados como protótipo | Primeiro RC ou necessidade de alterar o contrato de release | [ADR-003](docs/adr/ADR-003-versioning-and-public-api.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Checklist de release e baseline na #13 |
| D-006 | `engine.api.*` estável, `engine.internal.*` interno e `engine.incubator.*` experimental | @Balehlah (`technical-coordinator`) | 2026-07-24 | Visibilidade Java por si só não representa compromisso de compatibilidade | Pacotes legados atuais continuam protótipo até classificação na #13 | Vazamento transitivo ou promoção/rebaixamento de pacote | [ADR-003](docs/adr/ADR-003-versioning-and-public-api.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Baseline falha em quebra não permitida |
| D-007 | Resolução virtual provisória 320×180, configurável, nearest e escala inteira | @Balehlah (`technical-coordinator`) | 2026-07-24 | É uma referência 16:9 pequena, mensurável e já usada nos contratos do backlog | Barras centralizadas e degraded mode tornam-se requisitos | Evidência do spike ou testes de viewport invalidarem a escolha | [ADR-005](docs/adr/ADR-005-virtual-viewport.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Fixtures e conversões da #22 |
| D-008 | libGDX/LWJGL3 permanece experimental até passar integralmente o gate | @Balehlah (`technical-coordinator`) | 2026-07-24 | O protótipo Java2D não prova packaging, GPU, natives ou lifecycle multiplataforma | Migração horizontal fica proibida antes da decisão final | Conclusão ou inviabilidade do spike da #14 | [ADR-002](docs/adr/ADR-002-libgdx-lwjgl3-backend.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Matriz obrigatória da ADR-002; aprovador @Balehlah |
| D-009 | Decisões são históricas e só mudam por nova ADR | @Balehlah (`technical-coordinator`) | 2026-07-24 | Evita mudanças silenciosas nos contratos consumidos pelo backlog | Reversões preservam motivo, evidência e impacto | Nova evidência que torne uma decisão inadequada | [ADR-001](docs/adr/ADR-001-product-platform-runtime.md) | [#9](https://github.com/Balehlah/Engine_lite/issues/9) | Revisão do `technical-coordinator` e do `qa_validator` |
| D-010 | Windows e Linux são as únicas famílias desktop suportadas na 1.0.0; macOS não é suportado | @Balehlah (`technical-coordinator`) | 2026-07-29 | Não há ambiente disponível para validar a terceira família original; o runner Windows pode usar Mesa auditável e efêmero | D-002 é substituída sem reescrita; CI, pacote e gate passam a cobrir somente Windows/Linux | Nova família com ambiente de validação completo, perda de suporte do backend ou inviabilidade do tooling Mesa | [ADR-006](docs/adr/ADR-006-windows-linux-desktop-support.md) | [#60](https://github.com/Balehlah/Engine_lite/issues/60) | Dois checks verdes, quatro smokes, revisão `qa_validator` e aprovação do mantenedor |
| D-011 | libGDX/LWJGL3 é o backend desktop aceito para a linha 1.0.0; Java2D permanece fallback legado | @Balehlah (`technical-coordinator`) | 2026-07-30 | A matriz Windows/Linux passou integralmente em Java 21/25, com quatro smokes do ZIP e zero defeitos bloqueadores no QA independente | A escolha do backend está decidida; migração horizontal e remoção do legado continuam fora do spike | Regressão bloqueante no gate, incompatibilidade de licença/suporte ou nova necessidade arquitetural | [ADR-002](docs/adr/ADR-002-libgdx-lwjgl3-backend.md) | [#14](https://github.com/Balehlah/Engine_lite/issues/14) | PR #61 integrada; PR #59 verde; `qa_validator` e @Balehlah aprovam |

D-002 permanece acima como registro histórico imutável e é substituída por
D-010 para o contrato vigente. D-008 permanece como registro histórico do gate
experimental e é concluída por D-011, sem reescrita. D-009 continua aplicável.

## Estado dos ADRs

| ADR | Estado | Decisão |
|---|---|---|
| [ADR-001](docs/adr/ADR-001-product-platform-runtime.md) | Aceita | Produto, plataformas, Java e governança |
| [ADR-002](docs/adr/ADR-002-libgdx-lwjgl3-backend.md) | Aceita | libGDX/LWJGL3 aprovado; Java2D preservado como fallback legado |
| [ADR-003](docs/adr/ADR-003-versioning-and-public-api.md) | Aceita | SemVer, futura 1.0.0 e pacotes |
| [ADR-004](docs/adr/ADR-004-license-and-assets.md) | Aceita | Apache-2.0 e política de assets |
| [ADR-005](docs/adr/ADR-005-virtual-viewport.md) | Aceita provisoriamente | Viewport virtual 320×180 |
| [ADR-006](docs/adr/ADR-006-windows-linux-desktop-support.md) | Aceita | Substitui D-002: suporte desktop somente a Windows/Linux |

## Relação com o planejamento

- [PROJECT_AUDIT.md](PROJECT_AUDIT.md) registra a evidência anterior às decisões.
- [ROADMAP.md](ROADMAP.md) aplica os gates à ordem de execução.
- [README.md](README.md) comunica os contratos essenciais a consumidores.
- [CHANGELOG.md](CHANGELOG.md) separa marcos históricos do protótipo da futura
  linha SemVer.
- [docs/versioning.md](docs/versioning.md) detalha versionamento, depreciação e
  classificação de pacotes.
- [docs/validation/issue-9.md](docs/validation/issue-9.md) contém a revisão
  cruzada e as evidências dos critérios de aceite.
