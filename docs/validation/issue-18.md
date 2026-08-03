# Evidências de validação — Issue #18

- Data: 2026-08-03
- Branch: `codex/issue-18-typed-assets`
- Base consumida: `4f78de3`
- Issue: [#18](https://github.com/Balehlah/Engine_lite/issues/18)
- Estado: cinco critérios técnicos e gate canônico local concluídos

## Gate de entrada e escopo

- [x] #14 está fechada e ADR-002/D-011 aceita libGDX/LWJGL3.
- [x] #17 está fechada e seu contrato de `GameContext`, owners e cleanup está
  incorporado à `main`.
- [x] `engine:core` continua sem AWT, Swing, libGDX ou LWJGL.
- [x] Nenhuma dependência, plataforma, save, namespace estável ou API
  `engine.api.*` foi alterada.
- [x] A alteração staged preexistente em `.gitignore` permaneceu no checkout
  principal e não entrou na worktree desta issue.

O escopo novo fica em `engine.incubator.assets`; a implementação do backend
fica em `engine.incubator.gdx.assets`. AssetManager Java2D legado, demo,
hot reload universal, CDN e packing runtime não foram migrados ou modificados.

## Contrato executável

| Área | Política |
|---|---|
| ID | `AssetId<T extends SharedAssetData>` preserva nome lógico e tipo exato |
| Manifest | Imutável por grupo; rejeita duplicata, alias, absoluto e traversal |
| Source | Somente resolver `Classpath`/`Internal`; resolver dependente de CWD falha |
| Fallback | Selecionado antes da fila com diagnóstico determinístico |
| Async | `AssetLoad` + `CompletionStage`; `update` bombeia o AssetManager |
| Progresso | `AssetProgress` expõe contagem exata e fração por grupo |
| Concorrência | Produtores podem chamar `load`; backend e dispose ficam em uma thread |
| Compartilhamento | Mesmo source/tipo usa refcount entre grupos |
| Ownership | Serviço possui o AssetManager; grupo possui uma referência por entry |
| Handle | Vinculado à geração; unload/reload torna a geração anterior stale |
| Dados | Consumidor vê somente `SharedAssetData` read-only, nunca o disposer privado |
| Diagnóstico | Código, severidade, grupo, ID, source e mensagem; histórico limitado |
| Métricas | Pendências, grupos, referências, backend, falhas e stale observáveis |

## Critérios de aceite

- [x] **Tipo incorreto falha antes do uso.**
  `wrongTypeFailsBeforeTheBackendCanLoadOrReturnIt` rejeita um handle com token
  incorreto e um segundo manifest que tenta reservar o mesmo source com outra
  classe. A falha é `AssetFailure.TYPE_MISMATCH`, antes de novo load/get do
  AssetManager.
- [x] **Assets carregam fora do CWD.**
  `assetServiceCwdSmoke` inicia uma JVM com working directory real em
  `<isolatedBuildRoot>/engine/gdx/tmp/asset-service-external-cwd`, carrega
  `spike/sprite.rgba` do classpath, descarrega e retorna ao baseline.
  `classpathAssetAndManifestFallbackLoadOutsideTheWorkingDirectory` também
  protege seleção de fallback e o diagnóstico correspondente;
  `cwdDependentResolversAreRejectedBeforeBackendUse` rejeita resolvers
  `Absolute`, `Local` ou `External` antes do backend.
- [x] **Grupo descarrega cada recurso uma vez.**
  `sharedGroupsUnloadOneReferenceEachAndDisposeTheResourceOnce` carrega o mesmo
  source em dois grupos, comprova identidade compartilhada, fecha cada grupo
  duas vezes e observa um único dispose físico após a última referência.
- [x] **Handles stale falham de forma detectável.**
  `staleHandlesRemainDetectableAfterAGroupReload` fecha a geração 1, recarrega
  a geração 2 e confirma que o handle antigo continua falhando com
  `AssetFailure.STALE_HANDLE` e incrementa telemetria.
- [x] **20 trocas retornam contadores ao baseline.**
  `twentySceneChangesReturnEveryCounterToBaseline` liga o serviço ao execution
  owner da #17, registra cada grupo no scene owner e executa vinte transições.
  As 21 cenas/grupos terminam com loads=unloads, referências=0, assets backend=0
  e `ResourceMetrics.hasLeaks=false`.

## Testes e evidências obrigatórias

| Requisito | Evidência |
|---|---|
| Load/unload/reload | `staleHandlesRemainDetectableAfterAGroupReload` |
| Duplicatas | `duplicateIdsAndCandidateSourcesFailWhileBuildingTheManifest` e rejeição de grupo duplicado |
| Falha de path | `missingAndBrokenPathsFailWithDiagnosticsWithoutPoisoningTheNextLoad` |
| Concorrência | doze produtores concorrentes, um recurso físico e teardown exato |
| Lifecycle por grupo | dois grupos compartilhados e vinte transições via `GameRuntime` |
| Dados imutáveis | bound `SharedAssetData` e implementação disposable escondida por interface read-only |
| Globals | testes de reflexão rejeitam qualquer campo estático mutável novo |

Baseline anterior à implementação:

```text
gradlew.bat --no-daemon :engine:core:test :engine:gdx:test
  -PtestRandomSeed=1801
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue18-baseline
```

Resultado: `BUILD SUCCESSFUL`; 49 testes core e 27 GDX, zero falhas/erros.

Suite GDX direcionada com smoke de CWD real:

```text
gradlew.bat --no-daemon :engine:gdx:test
  -PtestRandomSeed=1818
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue18-gdx-full
```

Resultado: `BUILD SUCCESSFUL`; o task `assetServiceCwdSmoke` registrou
`asset-service-cwd-smoke=PASS` no diretório externo e os 35 testes GDX
passaram sem falha ou erro.

Gate canônico final desde `clean`, sem cache e com todas as tasks repetidas:

```text
gradlew.bat --no-daemon clean test --no-build-cache --rerun-tasks
  -PtestRandomSeed=18032026
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue18-final3-18032026
```

Resultado: `BUILD SUCCESSFUL` em 35 s; 41 tasks acionáveis, 35 executadas e
seis atualizadas. Relatórios JUnit:

| Módulo | Testes | Falhas/erros | Skips esperados |
|---|---:|---:|---:|
| `engine:core` | 54 | 0 | 0 |
| `engine:gdx` | 37 | 0 | 0 |
| `desktop` | 34 | 0 | 12 |

Também passaram `verifyBackendIndependence` sobre 52 fontes core,
`apiCheck`, `verifyPublicApiBoundaries`, inspeção dos cinco JARs,
licenças/assets, paridade das 37 fontes legadas para 44 classes e verificação
do ZIP Windows/Linux. O smoke forkado confirmou o CWD externo
`C:\tmp\engine-lite-issue18-final3-18032026\engine\gdx\tmp\asset-service-external-cwd`.

### Remediação do primeiro gate completo

A primeira execução completa terminou depois de todas as suites verdes porque
`verifyLegacyClassParity` ainda não classificava o novo package incubador e
reportou suas 17 classes como outputs inesperados. Resultado esperado: novos
contratos incubadores ficam fora do inventário histórico; observado: o gate
comparou-os com classes do protótipo legado. Impacto: integração bloqueada, sem
falha funcional ou mudança do baseline. A allowlist foi ampliada somente para
`engine/incubator/assets/**` e o gate inteiro foi repetido desde `clean` com o
resultado verde acima.

## Riscos residuais e rollback

Riscos não bloqueadores:

- loaders gráficos continuam exigindo que `update`, unload e `close` sejam
  chamados na thread do backend; o serviço detecta troca de thread;
- `SharedAssetData` é um contrato Java de imutabilidade transitiva, não uma
  prova automática do grafo inteiro; implementações novas exigem revisão;
- fallback cobre source ausente no preflight; conteúdo corrupto falha com
  diagnóstico e cleanup, sem escolher silenciosamente outro conteúdo.

Rollback: reverter o package core `engine.incubator.assets`, o adapter
`engine.incubator.gdx.assets`, seus testes/smoke, as três extensões mínimas do
build e esta documentação. O runtime da #17, spike da #14, assets existentes,
Java2D legado e `engine.api.*` permanecem intactos.

## Entrega e governança

Os cinco critérios técnicos e o gate canônico local estão concluídos. A CI
Windows/Linux e as revisões independentes devem permanecer verdes antes de
fechamento ou merge; nenhuma aprovação própria ou merge automático faz parte
desta entrega.
