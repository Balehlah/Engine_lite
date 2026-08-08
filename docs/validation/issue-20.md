# Evidências de validação — Issue #20

- Data: 2026-08-08
- Branch: `codex/issue-20-config-logging-metrics`
- Base consumida: `7608403`
- Issue: [#20](https://github.com/Balehlah/Engine_lite/issues/20)
- Estado: cinco critérios de aceite, evidências técnicas obrigatórias e CI
  remota Windows/Linux em Java 21/25 concluídos; revisões dos papéis responsáveis
  continuam gates de governança antes de merge/fechamento

## Gate de entrada e escopo

- [x] #15, #16 e #17 estão fechadas e incorporadas à `main`.
- [x] Os contratos consumidos de scheduler, input e `GameContext` existem.
- [x] `engine:core` permanece sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, plataforma, backend, viewport normativo ou API
  `engine.api.*` foi alterado.
- [x] A alteração staged preexistente em `.gitignore` permaneceu no checkout
  principal e não entrou nesta worktree.

O escopo novo do core fica em `engine.incubator.runtime.config`,
`engine.incubator.runtime.logging` e `engine.incubator.runtime.metrics`. A
implementação visual permanece no pacote incubador do spike libGDX e o launcher
desktop somente resolve application home, carrega a configuração e aplica os
campos da janela. Java2D legado, telemetria remota, cheats e editor de config não
foram modificados.

## Contrato executável

| Área | Política |
|---|---|
| Precedência | defaults `<` `config/engine.properties` `<` CLI, por campo |
| Falha | fail-fast antes do backend, sempre com campo e valor rejeitado |
| Paths | arquivo CLI absoluto; values relativos resolvidos pelo application home |
| Imutabilidade | `EngineConfig`, proveniência e snapshots são cópias imutáveis |
| Logging | clock/sink injetáveis; filhos contextuais sem singleton global |
| Contexto | `frame`/`tick` não negativos e `world` igual ao execution ID positivo |
| Métricas | FPS, UPS, frame, tick, catch-up, clamp, assets e draw calls no mesmo snapshot |
| Overlay | default desligado, CLI/config inicial e toggle `F3`; draw próprio excluído |
| Custo desligado | uma leitura `volatile` e branch, sem formatar strings ou desenhar |

## Critérios de aceite

- [x] **Config inválida falha com campo/valor.**
  `EngineConfigLoaderTest.invalidBoundariesAlwaysReportFieldAndRawValue`
  executa 17 valores inválidos cobrindo números, dimensões, booleanos, nível de
  log, janela de métricas e path vazio. Arquivo relativo explícito, campo
  desconhecido e override CLI duplicado também falham com
  `EngineConfigException.field()` e `rejectedValue()`.
- [x] **Precedência é testada e documentada.**
  O teste combina os três níveis e verifica a origem vencedora por campo.
  README, arquivo empacotado e este registro documentam a ordem. No smoke real,
  `runtime.updates-per-second` veio de `FILE`, enquanto
  `debug.overlay-enabled` e `paths.evidence-directory` vieram de `CLI`.
- [x] **Overlay não altera simulação.**
  `RuntimeDebugOverlayTest` executa sequências fixed-timestep idênticas com o
  overlay oculto e visível. O smoke compara `simulationTimeSeconds` e estado do
  sprite imediatamente antes/depois do draw e registra
  `overlay.simulation-unchanged=PASS`.
- [x] **Logs incluem frame/tick/world quando aplicável.**
  `EngineLoggerTest` cobre formatação e ausência de campos inventados;
  `GdxGameRuntimeLoopTest` usa o execution ID real. `runtime.log` do pacote
  contém, por exemplo, `frame=1 tick=0 world=1`.
- [x] **CWD alternativo funciona.**
  O ZIP foi extraído em diretório temporário, iniciado de outro diretório
  temporário fora do repositório e carregou
  `<application-home>/config/engine.properties`. `config.log` registrou
  `cwd-independent=true`; assets internos e evidências também passaram.

## Config boundaries, precedência e métricas falsas

O gate frio executou os testes de limites e a configuração default/file/CLI.
`FrameMetricsCollectorTest` usou `FakeNanoClock` e snapshots sintéticos para
observar exatamente 60 FPS, 120 UPS, frame 60, tick 120, três updates no frame,
dois hits de catch-up, 20 ms descartados, assets `1/2/4/3` e cinco draw calls.

`RuntimeDebugOverlayTest.formatsEveryRequiredMetricFromAFakeSnapshotWithoutLocaleDrift`
protege o texto completo do overlay com locale neutro.

## Benchmark do overlay desligado

Comando:

```text
gradlew.bat --no-daemon :engine:gdx:benchmarkDisabledOverlay
  -PisolatedBuildRoot=<worktree>/.issue20-build/benchmark
```

Resultado local Windows/Java 21:

```text
overlay.disabled.iterations=20000000;elapsed-nanos=11893800;
ns-per-check=0.595;renders=0
BUILD SUCCESSFUL
```

O número é evidência local, não um SLA cross-machine. O gate semântico é
`renders=0`; o benchmark também confirma que a API desligada não cria contexto
GL nem entra no formatter.

## Smoke externo e screenshot

Comando final:

```text
gradlew.bat --no-daemon :desktop:generateSpikeEvidenceManifest
  -PspikeSmokeVariant=issue20-final2-windows-java21
  -PtestRandomSeed=202020
  -PisolatedBuildRoot=<worktree>/.issue20-build/final2
```

Resultado: `BUILD SUCCESSFUL` e 20 artifacts hasheados. O processo observou CWD
temporário externo, carregou a configuração do pacote e concluiu três fixtures,
quatro eventos de input, zero overflow, zero eventos pendentes e onze recursos
liberados exatamente uma vez.

O screenshot `metrics-overlay.png` tem 1280×720 e mostra FPS `28.1`, UPS `63.2`,
frame `14`, tick `31`, cinco updates, três hits/`350.000 ms` de catch-up, alpha,
assets e dois draw calls. A inspeção visual confirmou texto legível no canto
superior esquerdo e o conteúdo pixel-art preservado.

| Artifact | SHA-256 |
|---|---|
| `metrics-overlay.png` | `06a84ad623828c148bef14b59128f22bd73faec71fea66f8df358898ea7aa764` |
| ZIP canônico | `f9fa2d4147a3eb20b3efbd853a188be1cdb49e47bc07864fb96c341547f7a055` |
| Manifest dos 20 artifacts | `aaab18244f2984ec9709431d475386a917a4e524a7b9c94b89b311ca25980439` |

## Gate canônico local

Comando final frio:

```text
gradlew.bat --no-daemon clean test --no-build-cache --rerun-tasks
  -PtestRandomSeed=20202004
  -PisolatedBuildRoot=<worktree>/.issue20-build/qa3
```

Resultado: `BUILD SUCCESSFUL` em 35 s; 41 tasks acionáveis, 35 executadas e seis
up-to-date.

| Módulo | Testes | Falhas/erros | Skips históricos esperados |
|---|---:|---:|---:|
| `engine:core` | 96 | 0 | 0 |
| `engine:gdx` | 40 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `verifyBackendIndependence` sobre 75 fontes core, `apiCheck`,
inspeção dos cinco JARs, licenças/assets, distribuição com o arquivo
`config/engine.properties`, paridade das 37 fontes legadas para 44 classes e
verificação dos relatórios JUnit/HTML.

A primeira execução completa encontrou somente a allowlist de paridade legada
sem os três novos namespaces incubadores. A allowlist foi ampliada de forma
específica e o gate inteiro foi repetido frio, com sucesso.

## CI remota

O PR draft [#67](https://github.com/Balehlah/Engine_lite/pull/67), commit de
código `b264657`, disparou a execução
[31278308081](https://github.com/Balehlah/Engine_lite/actions/runs/31278308081).
Os dois jobs obrigatórios terminaram com sucesso:

- [Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/31278308081/job/93155377283):
  testes completos em Java 21, reutilização do build cache, compatibilidade
  Java 25, distribuição e smokes do ZIP em Java 21/25;
- [Windows](https://github.com/Balehlah/Engine_lite/actions/runs/31278308081/job/93155377304):
  testes completos em Java 21, compatibilidade Java 25, distribuição, Mesa
  llvmpipe fixado com prova de rejeição de checksum e smokes do ZIP em Java
  21/25.

Ambos também verificaram que o ZIP canônico permaneceu inalterado e publicaram
relatórios JUnit/HTML, pacote e evidências do runtime.

## Riscos residuais e rollback

Riscos não bloqueadores:

- FPS/UPS usam janela configurável e portanto podem oscilar em janelas curtas;
- warnings de catch-up são emitidos somente quando o contador avança, limitando
  volume e cardinalidade;
- o spike publica zero para assets tipados porque ainda não migra seus recursos
  históricos para `GdxAssetService`; o adapter `AssetHealthMetrics.from`
  preserva os contadores reais para consumidores do serviço tipado.

Rollback: reverter os arquivos da Issue #20 remove os três namespaces do core,
config empacotada, logger/collector, novo overlay, integração do launcher e
evidências. Timing, input, lifecycle, assets tipados, eventos/IDs, Java2D legado
e API estável permanecem intactos.

## Gates de governança restantes

Antes de merge ou fechamento da issue, a branch publicada ainda deve receber
revisão do papel `engine-developer`, validação independente de `qa_validator` e
aprovação do `technical_coordinator`. Esses gates não são inferidos a partir da
validação local ou da CI e nenhum merge automático foi realizado.
