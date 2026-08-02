# Evidências de validação — Issue #16

- Data: 2026-08-02
- Branch: `codex/issue-16-input-snapshots`
- Base consumida: `84b90f7`
- Issue: [#16](https://github.com/Balehlah/Engine_lite/issues/16)
- Estado: implementação, validação local e revisão independente concluídas com
  `qa_validator` PASS; CI remota permanece gate de publicação

## Gate de entrada e escopo

- [x] #14 fechada e incorporada à `main`.
- [x] #15 fechada e incorporada à `main`.
- [x] ADR-002/D-011 mantém libGDX/LWJGL3 como backend aceito.
- [x] ADR-005 mantém viewport virtual configurável, escala inteira e barras.
- [x] `engine:core` permanece sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, plataforma, namespace estável ou API `engine.api.*`
  foi alterada.
- [x] A alteração preexistente em `.gitignore` permaneceu no checkout principal
  e não entrou nesta branch/worktree.

O contrato novo fica em `engine.incubator.runtime.input`; o adapter autorizado
fica em `engine.incubator.gdx.input`. O input Java2D legado, gamepads completos,
rebind de UI e replay em disco não foram migrados nem modificados.

## Contrato executável

| Área | Política |
|---|---|
| Fila | FIFO com capacidade padrão de 4.096 eventos |
| Coalescência | Somente movimentos absolutos consecutivos do ponteiro |
| Overflow | Fail-fast e contador observável; nenhuma borda é descartada silenciosamente |
| Snapshot | Record imutável, um por update lógico, com índice monotônico de tick |
| Teclas/botões | `down` persistente; `pressed` e `released` transitórios por um tick |
| Foco | Focus lost libera todos os controles mantidos e bloqueia presses tardios até focus gained |
| Mouse | Posição lógica/física/virtual, delta por tick, scroll acumulado por tick e região explícita |
| Resize/DPI | `ScreenToVirtual` é obtido no consumo do tick, não no callback |
| Fake input | Script em memória defensivamente copiado e reproduzido tick a tick |

## Critérios de aceite

- [x] **Press+release entre ticks preserva ambas as bordas.**
  `TickInputTest.pressAndReleaseBetweenTicksPreserveBothEdgesForExactlyOneTick`
  cobre teclado e mouse; a integração em catch-up é coberta por
  `GdxInputAdapterTest.catchUpConsumesOneImmutableSnapshotPerLogicalTick`.
- [x] **`pressed`/`released` duram exatamente um tick.** Os mesmos testes
  verificam que o snapshot seguinte não repete transientes; callbacks `down`
  repetidos não recriam `pressed`.
- [x] **Focus lost libera teclas sem stuck.** Testes core e GDX seguram tecla e
  botão, perdem foco, verificam `released` e rejeitam presses tardios enquanto
  sem foco.
- [x] **Mouse usa `screenToVirtual` e identifica barras.** Testes cobrem origem
  inferior esquerda, pillarbox/letterbox, resize e DPI 2×. O smoke físico
  confirma `BARS` para o primeiro probe e `VIEWPORT`/virtual `(10,139)` para o
  segundo.
- [x] **Fake input reproduz sequência idêntica.** O teste obrigatório executa
  1.000 replays completos em memória e compara todos os records de snapshot.

## Matriz obrigatória de testes

| Evidência | Teste/resultado |
|---|---|
| Eventos rápidos | Press+release de tecla e botão sobrevivem juntos em um tick |
| Repetição | `keyDown` repetido mantém `down` sem recriar `pressed` |
| Foco | Held state liberado; callback tardio ignorado; focus gained restaura entrada |
| Scroll | Eixos X/Y acumulam todos os eventos e zeram no tick seguinte |
| Movimento/delta | Movimentos consecutivos coalescem; delta compara posições virtuais dos respectivos snapshots, sem salto em resize/DPI |
| Resize/DPI | Transformação muda por tick e escala lógico→backbuffer antes do viewport |
| Barras | Quatro bordas e fora da superfície são distinguidos de `VIEWPORT` |
| 1.000 replays | Todas as sequências de `InputSnapshot` são idênticas |
| Overflow | Capacidade pequena força exceção e incrementa contador; bordas não coalescem |
| Catch-up | Dois updates no mesmo host frame recebem snapshots de ticks 0 e 1 |

## Execuções locais

Baseline anterior à implementação:

```text
gradlew.bat --no-daemon :engine:core:test :engine:gdx:test
  -PtestRandomSeed=1616
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue16-baseline
```

Resultado: `BUILD SUCCESSFUL`; 19 testes core e 21 testes GDX passaram, sem
falha ou skip.

Gate canônico após a implementação:

```text
gradlew.bat --no-daemon clean test
  -PtestRandomSeed=1619
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue16-final-1619
```

Resultado: `BUILD SUCCESSFUL` em 43 s; 40 tasks acionáveis, 28 executadas,
seis recuperadas do cache e seis atualizadas. Os relatórios JUnit registraram:

| Módulo | Testes | Falhas/erros | Skips esperados |
|---|---:|---:|---:|
| `engine:core` | 31 | 0 | 0 |
| `engine:gdx` | 26 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `verifyBackendIndependence` sobre 22 fontes core, `apiCheck`,
inspeção dos cinco JARs, licenças/assets, paridade das 37 fontes legadas para
44 classes e verificação do ZIP.

## Smoke do pacote

```text
gradlew.bat --no-daemon :desktop:generateSpikeEvidenceManifest
  -PspikeSmokeVariant=issue16-final-windows-java21
  -PtestRandomSeed=1619
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue16-final-1619
```

Resultado final: `BUILD SUCCESSFUL`; o ZIP foi extraído e iniciado de um CWD
temporário externo. O manifest contém 16 artifacts e o SHA-256 do ZIP é:

```text
800cdebea7686c1fe51c2465364b1586d0905723d66cefc9e29d730b23060bbf
```

`input.log` registrou capacidade 4.096, overflow fail-fast, coalescência apenas
de movimento adjacente, 27 ticks, quatro eventos aceitos, zero overflow e zero
pendentes. O probe físico pedido em `(17,29)` gerou região `BARS`, virtual
`(-1,-1)` e não moveu o sprite. O probe seguinte pedido em `(100,200)` gerou
região `VIEWPORT`, virtual `(10,139)` e moveu o sprite para offset 8.
`summary.properties` terminou `result=PASS`, `input.events=4`,
`input.queue.overflows=0`, `input.queue.pending=0`, três fixtures e dez recursos
liberados exatamente uma vez.

As fixtures 640×360, 800×600 e 1280×720 mantiveram escala/barras e golden
`PASS`. O primeiro smoke endurecido falhou porque interferência física entregou
o cursor quatro pixels distante do pedido enquanto o probe aceitava apenas
dois; os logs provaram região `BARS`. A tolerância ambiental foi ajustada para
oito pixels sem relaxar a exigência semântica de região, e a repetição passou.

## Revisão independente de QA

O `qa_validator` primeiro repetiu o gate e o smoke com seed próprio, depois
realizou revisão estática. A revisão rejeitou a primeira versão ao encontrar:

1. delta virtual artificial quando resize/DPI e movimento ocorriam no mesmo
   tick;
2. movimento em barras afetando o sprite no caminho interativo, embora o smoke
   especial o bloqueasse;
3. divergência entre `input.events`, `accepted-events` e um focus lost pendente
   no shutdown do smoke.

As três falhas foram corrigidas com regressões explícitas. O QA repetiu um gate
frio sem build cache e forçando todas as tasks:

```text
gradlew.bat --no-daemon clean test --no-build-cache --rerun-tasks
  -PtestRandomSeed=1616004
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue16-qa-1616004
```

Resultado: `BUILD SUCCESSFUL` em 42 s; core 31/31, GDX 26/26 e desktop
22 passes mais 12 skips históricos. Boundaries, API, JARs, licenças e
distribuição passaram. O smoke independente também passou de CWD externo com
16 artifacts, SHA-256 idêntico
`800cdebea7686c1fe51c2465364b1586d0905723d66cefc9e29d730b23060bbf`,
`input.events=accepted-events=4`, zero pendentes, zero overflow, barras e
viewport corretos e dez disposables liberados uma vez. Parecer final:
**PASS técnico, zero defeitos bloqueadores**.

## Riscos residuais e rollback

Riscos não bloqueadores:

- uma rajada acima de 4.096 eventos não coalescíveis falha de forma intencional
  em vez de perder bordas; capacidade e overflow ficam expostos para ajuste;
- movimento absoluto adjacente preserva posição final e delta líquido, mas não
  a trajetória intermediária, conforme a política autorizada;
- o smoke não força perda de foco real com um controle físico mantido; a
  transição `Application.pause()` → evento de foco está explicitamente
  configurada, e o reducer/adapter possuem cobertura direta sem janela.

Rollback: reverter os arquivos da Issue #16 remove o namespace de input, o
adapter e a integração pontual do spike. O loop fixo, Java2D legado, assets,
saves e API estável permanecem intactos.

## Gates externos restantes

Antes de fechar a issue, a branch publicada ainda deve receber CI verde em
Windows/Linux (Java 21 e smoke adicional em Java 25), revisão do papel
`engine-developer` e aprovação do `technical_coordinator`. Nenhum gate externo
é inferido a partir dos resultados locais.
