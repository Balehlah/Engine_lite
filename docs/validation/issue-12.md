# Evidências de validação — Issue #12

- Data: 2026-07-27
- Branch: `codex/issue-12-desktop-ci`
- Base consumida: `2dc46f7` (Issues #10 e #11 integradas em `main`)
- Pull request: [#57](https://github.com/Balehlah/Engine_lite/pull/57)
- Workflow: [`Desktop CI`](../../.github/workflows/build.yml)

## Critérios de aceite

- [x] Três checks aparecem no PR e `clean test` passa.
  Evidência: a
  [execução limpa `30296051190`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190)
  terminou verde em `Build and test (Ubuntu)`, `Build and test (Windows)` e
  `Build and test (macOS)`.
- [x] Falha de SO impede merge ou há proteção documentada.
  Evidência: a seção `Proteção de main` de
  [`docs/ci.md`](../ci.md#proteção-de-main) registra os três nomes que devem ser
  required checks e não usa filtro de paths. A consulta somente-leitura da
  proteção retornou HTTP 403 porque o plano atual do repositório não oferece o
  recurso; portanto a alternativa documentada do critério é a aplicável.
- [x] O workflow não chama `javac` nem scripts manuais.
  Evidência: todos os builds usam somente `gradlew` ou `gradlew.bat`; não há
  chamada a `build.sh`, `build.bat`, `run.sh` ou `run.bat`.
- [x] Os logs registram SO, JDK, Gradle e versão.
  Evidência: cada job registrou `runner.os`, `runner.arch`, a imagem nativa, a
  revisão Git do merge testado, Temurin `21.0.11` e Gradle Wrapper `9.6.1`.

## Execução verde multiplataforma

A execução limpa após o revert da falha controlada foi
[`30296051190`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190).
`fail-fast: false` preservou os três resultados independentes.

| Check | Runner | Resultado | Duração | Job |
|---|---|---:|---:|---|
| Ubuntu | `ubuntu-24.04` x64 | Verde | 1m42s | [`90077255044`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190/job/90077255044) |
| Windows | `windows-2025` x64 | Verde | 3m22s | [`90077255112`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190/job/90077255112) |
| macOS | `macos-15-intel` x64 | Verde | 2m10s | [`90077255129`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190/job/90077255129) |

Em todos os jobs:

- a validação automática do Wrapper passou;
- `clean test` terminou com `BUILD SUCCESSFUL`;
- o task separado `java25CompatibilityTest` confirmou XML JUnit e HTML de
  `engine:core` e `desktop`;
- o upload de relatórios terminou com sucesso.

## Relatórios por sistema operacional

Os artifacts da execução limpa ficam disponíveis na
[seção de artifacts do run](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190#artifacts)
por 14 dias.

| Artifact | ID | Tamanho | Conteúdo |
|---|---:|---:|---|
| `test-reports-Ubuntu` | `8664703187` | 133156 bytes | Java 21, Java 25 e HTML agregado |
| `test-reports-Windows` | `8664749389` | 133976 bytes | Java 21, Java 25 e HTML agregado |
| `test-reports-macOS` | `8664712323` | 134377 bytes | Java 21, Java 25 e HTML agregado |

Os gates `verifyJUnitReports` e `verifyJava25Reports` fazem a execução falhar se
os XMLs e HTMLs obrigatórios de `engine:core` ou `desktop` não existirem.

## Falha controlada

O commit temporário `eab23f9` adicionou a etapa
`Controlled Windows failure injection` somente ao item Windows. A
[execução `30295874687`](https://github.com/Balehlah/Engine_lite/actions/runs/30295874687)
produziu:

- Windows vermelho no passo controlado, limitado a um minuto;
- Ubuntu verde;
- macOS verde;
- upload executado com `if: always()` no sistema que falhou;
- execução completa dos outros sistemas, comprovando `fail-fast: false`.

O commit `4b39ccc` reverteu integralmente a injeção antes da execução verde
final. O commit temporário não deve ser integrado isoladamente.

## Cache miss/hit

O `setup-gradle` usa cache `basic`, valida o Wrapper e permanece
somente-leitura em PRs e merge queue. Somente pushes em `main` podem gravar no
cache compartilhado.

No job
[Ubuntu `90077255044`](https://github.com/Balehlah/Engine_lite/actions/runs/30296051190/job/90077255044),
a primeira execução de `clean test` populou o build cache local. A repetição
após novo `clean` registrou `FROM-CACHE` para:

- `:engine:core:compileJava`;
- `:desktop:compileLegacyJava`;
- `:desktop:compileTestJava`;
- `:desktop:test`;
- `:engine:core:compileTestJava`;
- `:engine:core:test`;
- `:game:compileJava`.

A repetição terminou em nove segundos, contra um minuto da primeira execução.
O resumo do `setup-gradle` registra separadamente o estado do cache remoto.

## Validação local

No Windows local, com Temurin 21.0.3 e diretórios descartáveis fora do
sincronizador:

```text
gradlew.bat --no-daemon tasks --all
gradlew.bat --no-daemon --stacktrace clean test \
  -PtestRandomSeed=1202 \
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue12-build
git diff --check
```

Resultado: `BUILD SUCCESSFUL`; 34 testes descobertos, 22 aprovados, 12
specifications futuras ignoradas, zero falhas; relatórios e gates das Issues
#10 e #11 confirmados.

## Risco residual, revisão e rollback

O smoke gráfico `legacyDemoSmoke` continua separado e não é gate inicial por
depender de display. Cada job e cada etapa de teste possuem timeout, e a
compatibilidade Java 25 não altera o bytecode mínimo Java 21.

Revisão deve ser solicitada aos papéis `devops-release` e `qa_validator`; esta
entrega não faz merge automático. O rollback consiste em reverter o workflow,
o task Java 25 e a documentação da Issue #12. Código de produção não foi
alterado.
