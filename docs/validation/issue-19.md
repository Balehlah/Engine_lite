# Evidências de validação — Issue #19

- Data: 2026-08-04
- Branch: `codex/issue-19-scoped-events-entity-ids`
- Base consumida: `7253f40`
- Issue: [#19](https://github.com/Balehlah/Engine_lite/issues/19)
- Estado: critérios técnicos e gate canônico local concluídos

## Gate de entrada e escopo

- [x] #9 e #10 estão fechadas e incorporadas à `main`.
- [x] `engine:core` continua sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, backend ou API estável `engine.api.*` foi alterada.
- [x] A alteração staged preexistente em `.gitignore` permaneceu no checkout
  principal e não entrou nesta branch/worktree.

O escopo novo fica em `engine.incubator.world.id` e
`engine.incubator.events`, com integração ao `WorldState`/`GameContext`
incubadores. Event sourcing, rede, scanning por reflexão, bus global, backend
e demo permanecem fora do escopo.

## Contrato executável

| Área | Política |
|---|---|
| ID | Value object positivo e comparável, estável durante a vida do mundo |
| Geração | `IdGenerator` injetável; sequência default crescente e reproduzível |
| Overflow | `Long.MAX_VALUE` é emitido uma vez; chamadas seguintes falham sem wrap |
| Colisão | O mundo reserva cada ID emitido até o close, inclusive após remove/unload |
| Escopo | Um gerador e um bus novos por mundo/restart; nenhum estado global |
| Tipagem | `EventType<T>` valida o payload antes de entrar na fila |
| Fases | Quatro filas explícitas; somente a fase solicitada é drenada |
| Ordem | Eventos FIFO; handlers na ordem de assinatura |
| Reentrância | Post durante dispatch vai ao fim da fila; dispatch recursivo falha |
| Handle | Unsubscribe/close idempotente e seguro durante dispatch |
| Cleanup | Unload remove handlers/eventos do owner; close invalida todos os handles |

## Critérios de aceite e testes obrigatórios

- [x] **IDs não colidem e a sequência é reproduzível.**
  `equalSeedsProduceTheSameCollisionFreeSequence` compara duas sequências de
  10.000 IDs; `aBrokenInjectedGeneratorCannotCreateAnIdCollision` prova o gate
  defensivo no `WorldState`. As regressões `anEmittedIdCannotBeReusedAfterEntityRemoval`
  e `anEmittedIdCannotBeReusedAfterOwnerUnload` preservam tombstones durante
  toda a vida do mundo.
- [x] **Ordem de handlers é documentada e determinística.**
  `eventsAreFifoPerPhaseAndHandlersFollowSubscriptionOrder` cobre FIFO de
  eventos, isolamento de fase e ordem de registro; a ordem normativa está em
  `docs/public-api.md` e no Javadoc de `WorldEventBus`.
- [x] **Unsubscribe durante dispatch é seguro.**
  `unsubscribeDuringDispatchSkipsAHandlerWhoseTurnHasNotStarted` remove o
  segundo handler pelo primeiro e comprova que ele não roda.
- [x] **Unload remove subscriptions.**
  `sceneUnloadRemovesItsSubscriptionsAndQueuedEvents` executa uma transição
  real de cena e observa handle inativo, evento antigo removido e apenas o
  handler da cena atual.
- [x] **Pacote não importa backend.**
  `:engine:core:verifyBackendIndependence` aprovou as 62 fontes core.

Evidências adicionais:

- geração: 10.000 IDs por seed e lookup bidirecional entidade/ID;
- overflow: `maximumValueIsReturnedOnceAndOverflowNeverWraps`;
- reentrância: `recursiveDispatchIsRejectedButPostedWorkRemainsFifo`;
- novas assinaturas durante dispatch: passam a valer somente no evento seguinte;
- 100 restarts: `oneHundredWorldRestartsResetIdsAndCloseEveryOldBus` cria 101
  mundos, reproduz o primeiro ID e invalida todos os buses/handles anteriores.

## Remediação da primeira revisão independente

As primeiras revisões de `engine-developer` e `qa_validator` bloquearam o SHA
`9b1eea2`: a verificação de colisão consultava somente entidades vivas, então
um gerador customizado poderia reutilizar um ID após `remove` ou unload. O
`WorldState` agora mantém tombstones em `issuedIds` por toda a vida do mundo;
a remoção da entidade não libera sua identidade. As duas regressões citadas
acima reproduzem os caminhos de remoção direta e unload de owner.

## Execuções locais

Suíte direcionada:

```text
gradlew.bat --no-daemon :engine:core:test
  --tests engine.incubator.runtime.lifecycle.WorldStateIdentityTest
  --tests engine.incubator.world.id.SequentialIdGeneratorTest
  --no-build-cache --rerun-tasks
  -PtestRandomSeed=1951
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue19-collision-fix
```

Resultado: `BUILD SUCCESSFUL`; oito testes direcionados passaram e
`verifyBackendIndependence` aprovou 62 fontes.

Gate canônico final:

```text
gradlew.bat --no-daemon clean test --no-build-cache --rerun-tasks
  -PtestRandomSeed=1957
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue19-review-fix-1957
```

Resultado: `BUILD SUCCESSFUL` em 37 s; 41 tasks acionáveis, 35 executadas e
seis atualizadas. Relatórios JUnit:

| Módulo | Testes | Falhas/erros | Skips esperados |
|---|---:|---:|---:|
| `engine:core` | 71 | 0 | 0 |
| `engine:gdx` | 37 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `apiCheck`, `verifyPublicApiBoundaries`, inspeção dos cinco
JARs, licenças/assets, paridade das 37 fontes legadas para 44 classes,
verificação da distribuição ZIP Windows/Linux e relatórios JUnit/HTML.

A linha local `java25CompatibilityTest` foi tentada, mas o host possui somente
a toolchain Java 21 e o build não configura download automático de JDK. O task
falhou antes de iniciar qualquer teste com “Cannot find a Java installation …
languageVersion=25”. A matriz Java 25 continua como verificação remota da CI;
isso não representa falha de código observada localmente.

## Riscos residuais e rollback

Riscos não bloqueadores: o bus é deliberadamente single-threaded; falha de um
handler é fail-fast e deixa os eventos seguintes na fila; IDs são únicos no
escopo do mundo, não entre mundos independentes.

Rollback: reverter os packages `engine.incubator.world.id` e
`engine.incubator.events`, restaurar `RuntimeEventQueue` e desfazer a integração
incubadora no lifecycle. Não há migração de dados, backend ou API estável.
