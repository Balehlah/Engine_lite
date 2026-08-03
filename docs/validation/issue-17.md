# Evidências de validação — Issue #17

- Data: 2026-08-02
- Branch: `codex/issue-17-game-context-lifecycle`
- Base consumida: `44a9098`
- Issue: [#17](https://github.com/Balehlah/Engine_lite/issues/17)
- Estado: implementação, validação local e CI remota Windows/Linux concluídas;
  revisões dos papéis responsáveis permanecem gates antes de merge/fechamento

## Gate de entrada e escopo

- [x] #14 está fechada como concluída e incorporada à `main`.
- [x] ADR-002/D-011 aceita libGDX/LWJGL3 e preserva Java2D como fallback.
- [x] `engine:core` permanece sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, plataforma, namespace estável ou API `engine.api.*`
  foi alterada.
- [x] A alteração staged preexistente em `.gitignore` permaneceu no checkout
  principal e não entrou nesta branch/worktree.

O contrato novo fica em `engine.incubator.runtime.lifecycle`; o adapter
autorizado fica em `engine.incubator.gdx.runtime`. O runtime Java2D legado, um
framework de DI, ECS genérico, editor e hot reload universal não foram migrados
ou modificados.

## Contrato executável

| Área | Política |
|---|---|
| Contexto | Uma instância nova de `GameContext` por execução/restart |
| Lifecycle | `create → enter → fixedUpdate/render → exit → dispose` |
| Trocas | Fila FIFO drenada somente fora de callbacks |
| Autoridade | `restart`/`close` apenas no host, fora de callbacks e cleanup |
| Ownership | Comparação por identidade; um owner ativo por recurso |
| Descarte | Uma tentativa por recurso, ordem reversa, owner idempotente |
| Falhas | Cleanup completo; falha primária preservada e demais suprimidas |
| Estado | Entidades, eventos e assets pertencem ao contexto/owner da execução |
| Métricas | Owners, registros, tentativas, sucessos, falhas e leaks observáveis |
| Backend | `GdxGameRuntimeLoop` liga o runtime ao `FixedTimestepLoop` |

## Critérios de aceite

- [x] **Dispose é idempotente e ocorre uma vez por owner.**
  `OwnedResourceRegistryTest.ownerDisposalIsReverseOrderedAndIdempotent`
  chama dispose duas vezes e depois fecha o registro; cada recurso é liberado
  uma vez em ordem reversa. O close repetido de runtime e adapter também é
  exercitado.
- [x] **Exceção não pula cleanup.**
  `GameRuntimeFailureTest` injeta falhas independentes em `create`, `enter`,
  `fixedUpdate`, `render`, `exit` e `dispose`. Em todas, a cena recebe um único
  dispose, o asset é liberado e o snapshot final possui zero entidades,
  eventos, assets, owners vivos ou recursos vazados. Um disposer que falha não
  impede os recursos/owners seguintes de serem processados.
- [x] **Trocas durante update são aplicadas na fronteira segura.**
  `callbackRequestedTransitionRunsOnlyAtTheSafeUpdateBoundary` registra
  `first.update.begin`, pedido de troca e `first.update.end` antes de qualquer
  `exit`; somente depois ocorrem cleanup e `second.create/enter`. Um segundo
  teste enfileira duas cenas no mesmo update e comprova a ordem FIFO completa.
- [x] **100 restarts não herdam entidades, eventos ou assets.**
  `oneHundredRestartsCreateFreshContextsWithoutStateOrAssetInheritance` cria
  101 contextos distintos, valida estado vazio no `create` de cada execução e
  termina com 101 assets liberados e zero leaks. O teste complementar executa
  100 trocas no mesmo contexto e mantém somente o estado do owner atual.
- [x] **Não há singleton mutável no runtime novo.**
  `NoMutableSingletonTest` inspeciona todos os tipos públicos do pacote e
  rejeita qualquer campo estático mutável.

## Remediação da primeira revisão independente

A primeira análise do `technical_coordinator` bloqueou o avanço por três
casos não cobertos: `restart`/`close` reentrantes durante callbacks, autoridade
pública de shutdown em `GameContext`/`OwnedResourceRegistry` e remoção por
`equals` na lista interna de `WorldState`. Os três foram corrigidos e protegidos
por regressão:

- `hostLifecycleCommandsAreRejectedInsideCallbacksWithoutMutatingExecution`
  comprova que comandos do host são rejeitados e não trocam nem fecham o
  contexto ativo;
- `LifecycleAuthorityTest` impede que contexto e registro voltem a expor
  `AutoCloseable`, `close` ou operações públicas de lifecycle de owners;
- `WorldStateIdentityTest` usa duas entidades distintas que são iguais por
  `equals` e comprova que a instância exata solicitada é removida.
- `resourceDisposersCannotReenterLifecycleOrMutateTheirDisposingOwner` protege
  todo o cleanup contra `close`, `restart`, `start` e `requestScene` reentrantes,
  rejeita assets tardios no owner em descarte e comprova que os recursos
  seguintes ainda são liberados com métricas limpas.

A suíte direcionada do pacote lifecycle executou 18 testes, com zero falhas,
erros ou skips. Uma segunda revisão dos três papéis permanece obrigatória antes
do fechamento.

## Ordem observada e falhas injetadas

Ordem protegida por teste para uma troca solicitada durante update:

```text
first.create
first.enter
first.update.begin
first.update.end
first.exit
first.dispose
first.asset.dispose
second.create
second.enter
second.render
second.exit
second.dispose
second.asset.dispose
```

Matriz de falhas: seis callbacks do lifecycle, um disposer de recurso e cleanup
de múltiplos owners. O registro continua após cada falha; `ResourceMetrics`
separa tentativas, disposes bem-sucedidos e falhas para que uma tentativa sem
sucesso permaneça visível como leak.

## Execuções locais

Suítes direcionadas:

```text
gradlew.bat :engine:core:test :engine:gdx:test
```

Resultado: `BUILD SUCCESSFUL`; todos os testes de core e GDX passaram, incluindo
os 19 casos novos (seis invocações parametrizadas de falha e quatro regressões
originadas nas revisões).

Gate canônico final:

```text
gradlew.bat --no-daemon clean test --no-build-cache --rerun-tasks
  -PtestRandomSeed=1717026
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue17-cleanup-boundary-final-1717026
```

Resultado: `BUILD SUCCESSFUL` em 35 s; 40 tasks acionáveis, 34 executadas e seis
atualizadas. Relatórios JUnit:

| Módulo | Testes | Falhas/erros | Skips esperados |
|---|---:|---:|---:|
| `engine:core` | 49 | 0 | 0 |
| `engine:gdx` | 27 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `verifyBackendIndependence` sobre 35 fontes core, `apiCheck`,
inspeção dos cinco JARs, licenças/assets, paridade das 37 fontes legadas para
44 classes e verificação do ZIP Windows/Linux. O primeiro gate revelou duas
allowlists de packages que ainda enumeravam apenas os incubadores de timing e
input; elas foram ampliadas exclusivamente para lifecycle/runtime e o gate
completo foi repetido desde `clean` com sucesso.

## Evidência remota

O PR draft [#64](https://github.com/Balehlah/Engine_lite/pull/64), commit
`8438892`, disparou a execução
[30768992253](https://github.com/Balehlah/Engine_lite/actions/runs/30768992253).
Os dois jobs obrigatórios terminaram com sucesso:

- [Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30768992253/job/91552704698):
  build, testes, compatibilidade e smokes aprovados em 2m26s;
- [Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30768992253/job/91552704649):
  os mesmos gates, incluindo o caminho específico do runner Windows, aprovados
  em 3m27s.

A solicitação formal de revisão dos papéis `engine-developer`, `qa_validator` e
`technical_coordinator` foi publicada na conversa do PR. O PR permanece draft
e nenhuma aprovação própria ou merge automático foi realizado.

## Riscos residuais e rollback

Riscos não bloqueadores:

- o runtime é deliberadamente single-threaded; callbacks de backend devem
  continuar entregando trabalho na thread lógica;
- falha de disposer é tentada uma única vez para preservar idempotência e fica
  observável como leak, sem retry implícito;
- o adapter está disponível para o backend aceito, mas a demo/spike existente
  não foi migrada horizontalmente nesta issue.

Rollback: reverter os arquivos da Issue #17 remove o namespace lifecycle, o
adapter GDX, seus testes e as duas extensões de allowlist. Timing, input,
Java2D legado, saves, assets existentes e API estável permanecem intactos.

## Gates externos restantes

Antes de fechar a issue, o PR ainda deve receber revisão do papel
`engine-developer`, validação independente do `qa_validator` e aprovação do
`technical_coordinator`. A CI Windows/Linux está verde; nenhum gate humano é
inferido a partir da validação local ou remota automatizada.
