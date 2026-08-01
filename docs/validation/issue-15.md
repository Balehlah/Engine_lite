# Evidências de validação — Issue #15

- Data: 2026-08-01
- Branch: `codex/issue-15-fixed-timestep`
- Base consumida: `4e29647`
- Issue: [#15](https://github.com/Balehlah/Engine_lite/issues/15)
- Estado: implementação e validação local concluídas com parecer independente
  `qa_validator` PASS; CI remota permanece gate de publicação

## Gate de entrada e escopo

- [x] #14 fechada como concluída e incorporada à `main`.
- [x] ADR-002/D-011 aceita libGDX/LWJGL3 e preserva Java2D como fallback.
- [x] `engine:core` permanece sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, plataforma, namespace estável ou API `engine.api.*`
  foi alterada.
- [x] A alteração staged preexistente em `.gitignore` foi preservada fora do
  escopo da Issue #15.

O scheduler foi adicionado ao namespace experimental
`engine.incubator.runtime.time`. O adapter e o overlay permanecem no pacote
incubador autorizado de `engine:gdx`. O Java2D legado, colisão, rollback de
networking e sincronização de áudio não foram migrados ou modificados.

## Política executável

| Campo | Padrão | Semântica |
|---|---:|---|
| Frequência lógica | 60 Hz | Todo callback recebe `1.0 / 60.0` segundo |
| Período do clock | 16.666.667 ns | Usado apenas para o acumulador/`alpha` |
| Clamp por frame | 250 ms | Excesso de wall time é descartado e medido |
| Catch-up | 5 updates | Backlog integral excedente é descartado; a fração para `alpha` é preservada |
| `alpha` | `[0, 1)` | Calculado depois do update e dos descartes |
| Pause/escala zero | sem acumulação | Wall time inativo é descartado e contabilizado |
| Single-step | 1 update por frame solicitado | Disponível somente pausado; não consome wall time oculto |

As políticas são imutáveis depois da construção de `FixedTimestepConfig`. O
time scale altera a frequência com que passos ficam disponíveis, nunca o `dt`
entregue ao update.

## Critérios de aceite

- [x] **Update recebe sempre o mesmo dt.**
  `FixedTimestepLoopTest.everyCatchUpUpdateReceivesTheSameConfiguredDeltaThenRendersOnce`
  executa três updates de catch-up e compara os três valores com o `dt`
  configurado.
- [x] **Render ocorre mesmo sem update.**
  `FixedTimestepLoopTest.rendersEvenWhenNoLogicalUpdateIsDue` observa zero
  updates e exatamente um callback de render.
- [x] **Catch-up respeita limite e reporta descarte.**
  `FixedStepAccumulatorTest.longStallIsClampedAndCatchUpIsBoundedWithExplicitDiscardMetrics`
  injeta stall de um segundo, limita a três updates e verifica 900 ms de clamp,
  dois passos descartados e `alpha` dentro da fronteira.
- [x] **Fake clock reproduz snapshots idênticos.**
  `FixedTimestepSchedulerTest.identicalFakeClockSequencesProduceIdenticalSnapshots`
  compara records completos; o teste de 10.000 ticks repete duas execuções e
  obtém estado e métricas idênticos.
- [x] **Pause/step não acumulam tempo oculto.**
  Os testes `pauseAndSingleStepDiscardHiddenWallTimeWithoutChangingPartialAlpha`
  e `pauseAndZeroScaleTransitionsDiscardTimeEvenWithoutHostFrames` injetam
  horas sem frames, executam single-step e confirmam que somente o passo
  solicitado entra na simulação.

## Sequências e limites obrigatórios

| Evidência | Resultado |
|---|---|
| Deltas sintéticos | Duas sequências idênticas geraram `FrameSchedule`/`SchedulerMetrics` idênticos |
| Long stall | Clamp e limite de catch-up aplicados com telemetria separada |
| 10.000 ticks | 10.000 updates, 10.000 frames, snapshots repetidos idênticos |
| `alpha` | Imediatamente antes do passo: `< 1`; na fronteira: `0`; todas as sequências em `[0, 1)` |
| Time scale | `0.5x` muda frequência sem mudar o passo lógico; `0x` não cria backlog |
| Relógio inválido | Rewind, deltas negativos, overflow e escalas inválidas são rejeitados |

## Execuções locais

Baseline anterior à implementação:

```text
gradlew.bat --no-daemon :engine:core:test :engine:gdx:test
  -PtestRandomSeed=1515
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue15-baseline
```

Resultado: `BUILD SUCCESSFUL` em 17 s; os 4 testes de core e 16 testes GDX
preexistentes passaram.

Gate canônico final:

```text
gradlew.bat --no-daemon clean test
  -PtestRandomSeed=1515
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue15-final2
```

Resultado: `BUILD SUCCESSFUL` em 29 s; 40 tasks acionáveis, 26 executadas, 8
recuperadas do cache e 6 atualizadas. Os relatórios JUnit registraram:

| Módulo | Testes | Falhas/erros | Skips esperados |
|---|---:|---:|---:|
| `engine:core` | 19 | 0 | 0 |
| `engine:gdx` | 21 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `verifyBackendIndependence` sobre 15 fontes core,
`apiCheck`, inspeção dos cinco JARs, licenças/assets, paridade das 37 fontes
legadas para 44 classes e verificação do ZIP.

Smoke do pacote:

```text
gradlew.bat --no-daemon :desktop:generateSpikeEvidenceManifest
  -PspikeSmokeVariant=issue15-windows-java21
  -PtestRandomSeed=1515
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue15-final2
```

Resultado: `BUILD SUCCESSFUL`; o ZIP iniciou a partir de CWD temporário externo,
encerrou automaticamente e gerou 15 artifacts com manifesto SHA-256. O
`summary.properties` registrou `result=PASS`, três fixtures e os dez recursos
possuídos liberados exatamente uma vez. Os três viewports/goldens permaneceram
`PASS`.

SHA-256 do ZIP validado depois do endurecimento do gate de timing:
`11e11f05798a2a53cf5f88e385e648420c4fe85045eef23ce13b9ae4539efed3`.

O `timing.log` do smoke registrou:

```text
fixed.updates-per-second=60.0
fixed.dt-seconds=0.016666666666666666
fixed.step-nanos=16666667
clamp-nanos=250000000
max-catch-up=5
frames=12
updates=24
alpha=0.39547907209041855
clamped-frames=2
clamped-wall-nanos=127284800
catch-up-limit-hits=3
catch-up-discarded-nanos=366666674
inactive-wall-nanos=93334200
```

Os três hits de catch-up vieram das pausas reais para resize/captura do smoke e
demonstram que o pacote integrado aplicou o limite e expôs o descarte. O overlay
é renderizado apenas no modo interativo; no smoke ele permanece oculto para
preservar os goldens, enquanto a mesma telemetria é gravada em `timing.log`.

## Revisão independente de QA

O papel `qa_validator` repetiu a validação com seed `851501` e build roots
isolados. O parecer foi **PASS técnico, zero defeitos bloqueadores**.

```text
gradlew.bat --no-daemon clean test
  -PtestRandomSeed=851501
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue15-qa-final-851501
```

Resultado: `BUILD SUCCESSFUL`; 74 testes, zero falhas/erros: core 19/19, GDX
21/21 e desktop com 22 passes mais 12 skips históricos.

O QA também repetiu `:engine:gdx:test --rerun-tasks`, executou o smoke/manifest
do ZIP e realizou um probe externo de 100.000 frames, incluindo 10.000 ticks,
stall longo, pause/step e `timeScale=0`. O probe terminou com:

```text
PASS seed=851501 frames=100000 updates=176255 checksum=-7564878849341539461
```

No smoke independente, `timing.log` continha exatamente duas linhas e registrou
12 frames, 23 updates, `alpha=0.2470451950590961`, três hits do limite e
283.333.339 ns descartados. Os 15 artifacts, três goldens, dez disposables e a
integridade antes/depois do ZIP passaram.

Durante a revisão, o QA identificou que `timing.log` não era limpo nem exigido
pelo manifest e que a primeira versão do parser não reconhecia o timestamp.
Esses defeitos foram corrigidos com limpeza explícita, teste contra telemetria
obsoleta, requisito obrigatório e parsing estrutural de exatamente uma linha de
política e uma linha de métricas; toda a matriz local foi repetida após a
correção.

A primeira tentativa do smoke independente recebeu 50 eventos físicos externos
de cursor e expirou no probe de input. A repetição idêntica sem interferência
passou; o achado foi classificado como flake ambiental preexistente e não como
regressão da Issue #15.

## Riscos residuais e rollback

Riscos não bloqueadores:

- descartar backlog evita spiral of death, mas reduz o tempo simulado sob carga;
  as duas categorias de descarte ficam explícitas para diagnóstico;
- o scheduler entrega `alpha`, mas interpolação de estado/colliders permanece
  corretamente fora do escopo;
- Java 25 e a matriz Linux dependem da CI, pois as coletas locais principal e
  independente usam Java 21 no Windows.

Rollback: reverter os arquivos da Issue #15 remove o namespace de timing,
adapter, overlay e integração pontual no `render()` do spike. A aplicação volta
ao render direto anterior, sem migração de save/assets e sem tocar no fallback
Java2D.

## Gates externos restantes

Antes de fechar a issue, a branch publicada ainda deve receber CI verde em
Windows/Linux (Java 21 e smoke adicional em Java 25) e aprovação do
`technical_coordinator`. A validação independente de `qa_validator` foi
concluída localmente; os gates remotos não foram inferidos a partir dela.
