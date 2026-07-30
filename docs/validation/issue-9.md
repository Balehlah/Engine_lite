# Evidências de validação — Issue #9

> Registro histórico fechado. O contrato de plataformas validado nesta issue
> foi substituído por D-010/ADR-006 sem alterar esta evidência.

- Data: 2026-07-24
- Responsável: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Escopo: decisões e documentação de fundação
- Issue: [#9](https://github.com/Balehlah/Engine_lite/issues/9)

## Aprovação humana

O mantenedor aprovou explicitamente, em 2026-07-24, o pacote completo de nove
decisões antes de qualquer alteração no repositório. A implementação preservou
as escolhas aprovadas sem incluir build, backend ou runtime.

## Critérios de aceite

- [x] Todas as decisões possuem escolha, responsável, data e justificativa.
  Evidência: nove linhas D-001 a D-009 no
  [registro mestre](../../ARCHITECTURE_DECISIONS.md#registro-mestre).
- [x] ADR-002 mantém gate mensurável, aprovador e fallback até o spike.
  Evidência: [matriz e regra de decisão](../adr/ADR-002-libgdx-lwjgl3-backend.md).
- [x] A política explica a futura 1.0.0 apesar do changelog histórico.
  Evidência: [política de versionamento](../versioning.md) e
  [changelog](../../CHANGELOG.md#política-da-futura-100).
- [x] Pacotes estáveis, internos e incubadores ficam explicitamente definidos.
  Evidência: [classificação normativa](../versioning.md#classificação-normativa-de-pacotes).

## Tabela decisão → ADR → issue → gate

A tabela normativa está em
[ARCHITECTURE_DECISIONS.md](../../ARCHITECTURE_DECISIONS.md#registro-mestre).
Ela contém, para cada decisão:

- identificador;
- escolha;
- responsável;
- data;
- justificativa;
- consequência;
- gatilho de revisão;
- ADR;
- issue;
- gate verificável.

## Revisão cruzada dos quatro documentos de planejamento

Documentos revisados:

1. [PROJECT_AUDIT.md](../../PROJECT_AUDIT.md)
2. [ARCHITECTURE_DECISIONS.md](../../ARCHITECTURE_DECISIONS.md)
3. [ROADMAP.md](../../ROADMAP.md)
4. [README.md](../../README.md)

| Contrato | Auditoria | Decisões | Roadmap | README | Resultado |
|---|---|---|---|---|---|
| Motor é produto; demo é consumidora | Estado observado e decisão listada | D-001 | Princípio 1 | Estado do projeto | Consistente |
| Windows, Linux e macOS | Evidência e decisão | D-002 / ADR-001 | Gates #12/#14 | Contratos e requisitos | Consistente |
| Java 21 baseline; Java 25 compatibilidade | Decisão fechada | D-003 / ADR-001 | Gates #10/#12 | Contratos e requisitos | Consistente |
| Apache-2.0 e assets rastreáveis | Risco e encaminhamento | D-004 / ADR-004 | Gate #13 | Estado da licença | Consistente |
| Futura 1.0.0 inaugura SemVer | Decisão fechada | D-005 / ADR-003 | Seção de release | Estado do projeto | Consistente |
| API estável, interna e incubadora | Decisão fechada | D-006 / ADR-003 | Gate #13 | Namespaces explícitos | Consistente |
| Viewport 320×180 provisório | Risco e encaminhamento | D-007 / ADR-005 | Gate #22 | Contrato de fundação | Consistente |
| Backend experimental com fallback | Risco e encaminhamento | D-008 / ADR-002 | Gate #14 | Java2D identificado como legado | Consistente |
| Reversão somente por nova ADR | Decisão fechada | D-009 / governança | Princípio 5 | Link para fonte normativa | Consistente |

Documentos suplementares revisados:

- [CHANGELOG.md](../../CHANGELOG.md) usa `prototype-1.0.0` e
  `prototype-2.0.0`, preservando o histórico sem conflitar com a futura
  `v1.0.0`.
- [docs/versioning.md](../versioning.md) detalha SemVer, depreciação e
  classificação de pacotes.
- Os cinco ADRs aparecem no índice e possuem estado, data, responsável,
  justificativa, consequências e gatilhos de revisão.

## Validações executadas

| Validação | Resultado |
|---|---|
| `git diff --check` | Passou |
| Presença de D-001 a D-009 | Passou; nove decisões encontradas |
| Metadados dos cinco ADRs | Passou |
| Compilação isolada com `javac 21.0.3 --release 21` | Passou com exit code 0 |
| Alterações em `src/`, `bin/`, `build.*` ou `run.*` | Nenhuma |
| Links Markdown relativos | Passou |
| CI relevante | Não existe workflow na baseline; criação da CI pertence à Issue #12 |

A compilação foi direcionada a um diretório temporário e removida depois da
execução; nenhum binário versionado foi regravado.

## Risco residual

- A licença ainda não está materializada em `LICENSE`; essa implementação é
  gate da Issue #13.
- Java 25 e os três sistemas operacionais ainda não têm CI; essa evidência é
  responsabilidade da Issue #12.
- libGDX/LWJGL3 continua experimental até a matriz da ADR-002 ser executada pela
  Issue #14.
- 320×180 continua provisório até os testes da Issue #22.

Esses riscos são dependências posteriores documentadas, não critérios pendentes
da Issue #9.

## Rollback

Como esta mudança é apenas documental, o rollback técnico consiste em reverter
os arquivos desta tarefa. Depois de aceitas e publicadas, as decisões não devem
ser apagadas: qualquer mudança de conteúdo normativo deve ocorrer por nova ADR,
preservando o histórico.
