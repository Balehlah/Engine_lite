# Evidências de validação — Issue #11

- Data: 2026-07-26
- Branch: `codex/issue-11-baseline-tests`
- Base consumida: `3457393` (Issue #10 fechada)
- Ambiente local: Windows NT 10.0.26200.0 amd64, Temurin 21.0.3 LTS
- Issue: [#11](https://github.com/Balehlah/Engine_lite/issues/11)

## Registro inicial dos defeitos

Os defeitos foram reproduzidos antes da criação da suíte. O probe foi compilado
com `javac --release 21`, executado em modo headless e não alterou código de
produção. A saída integral está em
[`initial-reproduction.log`](issue-11/initial-reproduction.log).

| ID | Ambiente e passos | Resultado esperado | Resultado observado | Impacto | Destino futuro |
|---|---|---|---|---|---|
| `INPUT-EDGE` | Enfileirar press+release de teclado e mouse antes de `update()` | `pressed` e `released` preservam ambas as bordas por um tick | `down=false`, `pressed=false` e `released=false` | Ações rápidas são perdidas | #16 |
| `INPUT-DELTA` | Mover o mouse para `(8,5)` e chamar `update()` | Delta `(8,5)` disponível no tick | Delta `(0,0)` | Movimento entre ticks é descartado | #16 |
| `ANIM-ONE` | Atualizar `LOOP_PINGPONG` com um único frame por uma duração | Índice permanece `0` | Índice torna-se `-1` | O próximo acesso ao sprite falha | #41 |
| `RAY-STATE` | Acertar um AABB e reutilizar o raio contra um AABB ausente | Miss limpa todos os campos do hit anterior | Retorno é `false`, mas `hit`, `point`, `normal`, `distance` e `hitObject` permanecem | Consumidor lê colisão fantasma | #28/#31 |
| `RAY-ZERO` | Testar círculo com direção `(0,0)` | Entrada inválida falha sem resultado numérico inválido | Retorna hit com distância e ponto `NaN` | Estado físico contaminado por `NaN` | #28/#31 |
| `AABB-FINITE` | Construir AABB com `x=NaN` e largura negativa | Shape rejeita valores não finitos e dimensões inválidas | Instância é aceita | Interseções deixam de ter semântica confiável | #28 |
| `AABB-ZERO` | Consultar MTV entre AABBs apenas encostados | Resultado sem penetração é `Vector2.ZERO` canônico | MTV contém `y=-0.0` e não é igual a `Vector2.ZERO` | Resultados equivalentes a zero divergem em igualdade/hash | #28 |
| `SWEEP-ZERO` | Fazer sweep de AABBs sobrepostos com velocidade zero | Movimento zero vira overlap finito | Resultado é `NaN` por divisão `0/0` | Solver recebe tempo de impacto inválido | #28/#31 |
| `TILE-NEG` | Converter mundo `(-1,-1)` em tiles de 16 px | Piso matemático produz `(-1,-1)` | Cast truncado produz `(0,0)` | Coordenadas fora do mapa acessam o primeiro tile | Correção futura de tilemap |
| `SERIAL-ROUNDTRIP` | Salvar cena `Cena ação` com entidade `Herói` e recarregar | Nomes preservam seus escopos | Cena vira `Herói`; entidade volta como `Entity` | Round-trip perde identidade | Correção futura de serialização |
| `SERIAL-INVALID` | Carregar arquivo com header inválido e `x=oops` | Formato é rejeitado de modo explícito e controlado | `NumberFormatException` escapa | Conteúdo inválido derruba o carregamento | Correção futura de serialização |

## Comportamento útil confirmado

O mesmo probe confirmou que o acumulador de scroll preserva múltiplas unidades
até o próximo `update()` (`scroll=2`). Esse contrato será protegido como
`specification`, assim como matemática vetorial e colisões válidas. O teste de
round-trip protege os campos que o protótipo preserva e caracteriza
separadamente a perda dos nomes.

## Matriz achado → teste → status

| Achado/contrato | Teste JUnit | Tag | Status |
|---|---|---|---|
| `INPUT-EDGE` teclado | `quickKeyboardPressAndReleaseDisappearAtTheDocumentedUpdateBoundary` | `characterization` | Passa reproduzindo a perda; specification de #16 desabilitada |
| `INPUT-EDGE` mouse | `quickMousePressAndReleaseDisappearAtTheDocumentedUpdateBoundary` | `characterization` | Passa reproduzindo a perda; specification de #16 desabilitada |
| `INPUT-DELTA` | `mouseMovementBeforeUpdateIsCollapsedToZeroDelta` | `characterization` | Passa reproduzindo delta zero; specification de #16 desabilitada |
| Scroll por tick | `scrollAccumulatesUntilUpdateAndLastsExactlyOneTick` | `specification` | Passa |
| Estado global de input | `InputGlobalStateIsolationTest` | `specification` | Dois testes passam com reset antes/depois e `ResourceLock` |
| `ANIM-ONE` | `oneFramePingPongMovesToNegativeFrameAndBreaksSpriteAccess` | `characterization` | Passa reproduzindo índice `-1`; specification de #41 desabilitada |
| `RAY-STATE` | `missAfterHitRetainsEveryFieldFromThePreviousHit` | `characterization` | Passa reproduzindo estado retido; specification de #28/#31 desabilitada |
| `RAY-ZERO` | `zeroDirectionCircleQueryReportsAHitContainingNaN` | `characterization` | Passa reproduzindo `NaN`; specification de #28/#31 desabilitada |
| `AABB-FINITE` | `constructorAcceptsNaNAndNegativeDimensions` | `characterization` | Passa reproduzindo shape inválido; specification de #28 desabilitada |
| `AABB-ZERO` | `nonIntersectingMtvContainsNegativeZeroAndIsNotVectorZero` | `characterization` | Passa reproduzindo `-0.0`; specification de #28 desabilitada |
| AABB/Collision válidos | `touchingEdgesDoNotCountAsIntersection`, `overlapReturnsTheMinimumTranslationVector` | `specification` | Passam |
| `SWEEP-ZERO` | `zeroVelocitySweepOfOverlappingBoxesReturnsNaN` | `characterization` | Passa reproduzindo `NaN`; specification de #28/#31 desabilitada |
| `TILE-NEG` | `negativeWorldCoordinateIsTruncatedIntoTileZero` | `characterization` | Passa reproduzindo tile `(0,0)`; specification futura desabilitada |
| Tile positivo | `positiveWorldCoordinatesRespectTileBoundaries` | `specification` | Passa |
| `SERIAL-ROUNDTRIP` | `sceneRoundTripOverwritesSceneNameAndDropsEntityName` | `characterization` | Passa reproduzindo perda de nomes; specification futura desabilitada |
| `SERIAL-INVALID` header | `invalidHeaderIsIgnoredAndItsEntitiesAreAccepted` | `characterization` | Passa reproduzindo aceitação; specification futura desabilitada |
| `SERIAL-INVALID` número | `invalidNumericValueEscapesAsNumberFormatException` | `characterization` | Passa reproduzindo exceção não controlada |
| Matemática vetorial | `Vector2SpecificationTest` | `specification` | Três testes passam |

## Execuções com ordem variável

As duas execuções usaram `clean`, build cache desabilitado e seeds diferentes.
Ambas descobriram 34 testes: 22 passaram, 12 specifications futuras foram
ignoradas e nenhuma falhou. A ordem realmente mudou: no desktop, a seed 1101
começou por `AnimationCharacterizationTest`; a 1129 começou por
`InputCharacterizationTest` e terminou por animação.

| Seed | Resultado | Evidência |
|---|---|---|
| `1101` | Exit 0; `BUILD SUCCESSFUL`; todos os gates passam | [`test-seed-1101.log`](issue-11/test-seed-1101.log) |
| `1129` | Exit 0; `BUILD SUCCESSFUL`; ordem diferente; todos os gates passam | [`test-seed-1129.log`](issue-11/test-seed-1129.log) |

A worktree desta validação fica sob OneDrive, que converteu outputs intermediários
em reparse points. As execuções finais usaram o parâmetro documentado
`isolatedBuildRoot` apenas para colocar diretórios descartáveis de build em
`C:\Dev\Engine_lite-issue11-build`; o default do projeto continua
`<projeto>/build`.

## Mutação controlada

`Vector2.add(Vector2)` foi temporariamente mutado de soma para subtração. O
teste focado `arithmeticReturnsNewValuesWithoutMutatingTheOperands` falhou
sozinho (exit 1), provando sensibilidade. A soma foi restaurada imediatamente,
e o arquivo de produção não aparece no diff final. Evidência:
[`controlled-mutation.log`](issue-11/controlled-mutation.log).

## Relatórios publicados

O task `verifyJUnitReports` confirmou XML e HTML de `engine:core` e `desktop`.
O task `aggregateTestReport` publicou a visão consolidada. Na execução final:

- 8 arquivos JUnit XML;
- 34 testes, 0 falhas, 0 erros e 12 ignorados;
- HTML por módulo em `<buildDir>/reports/tests/test/index.html`;
- HTML agregado em `<buildDir-raiz>/reports/tests/aggregate/index.html`.

Os caminhos e o uso em CI estão documentados em
[`docs/testing.md`](../testing.md).

## Critérios de aceite

- [x] `gradlew test` passa repetidamente e sem dependência de ordem.
- [x] Cada defeito crítico possui reprodução ou specification test vinculado.
- [x] Estado global não vaza entre testes.
- [x] Relatórios JUnit XML e HTML são publicados pelo build.

## Risco residual e rollback

Os testes de caracterização descrevem defeitos, não os legitimam como API. Cada
expectativa futura ficará explícita e desabilitada até a issue responsável
corrigir a produção. O rollback consiste em reverter somente configuração de
testes, fontes de teste e esta documentação; nenhum arquivo em `src/engine/**`
faz parte da entrega.
