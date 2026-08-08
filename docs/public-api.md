# Contrato da API pública

Este documento aplica a
[ADR-003](adr/ADR-003-versioning-and-public-api.md) sem estabilizar
acidentalmente os pacotes públicos do protótipo.

## Superfície protegida

Somente tipos em `engine.api.*` pertencem à API estável. A baseline inicial
contém `engine.api.EngineVersion`; os pacotes legados continuam sem compromisso
de compatibilidade.

| Namespace | Contrato |
|---|---|
| `engine.api.*` | API estável, coberta por SemVer e pela baseline |
| `engine.internal.*` | Implementação sem garantia; proibida em assinaturas estáveis |
| `engine.incubator.*` | API experimental; mudanças exigem changelog e migração |
| demais pacotes `engine.*` atuais | Protótipo legado, ainda não estabilizado |
| `game.*` | Demo/consumidor; nunca é API do motor |

A baseline é preparatória enquanto a versão está em
`1.0.0-SNAPSHOT`. O primeiro RC congela formalmente a superfície. Até esse
freeze, qualquer atualização continua exigindo revisão explícita do
`technical-coordinator`; depois da 1.0.0, uma quebra exige nova major.

## Gate de assinatura

O task `:engine:core:apiCheck` empacota `engine:core`, extrai com o `javap` do
JDK 21 todas as assinaturas públicas e protegidas de tipos acessíveis em
`engine.api.*` e compara o resultado com
[`gradle/public-api-baseline.txt`](../gradle/public-api-baseline.txt). Remoções,
renomes, mudanças de descritor, herança ou visibilidade fazem o gate falhar.

Uma alteração intencional segue esta ordem:

1. classificar a mudança segundo SemVer;
2. obter aprovação do `technical-coordinator`;
3. atualizar código, documentação, changelog e migração;
4. executar `gradlew.bat :engine:core:apiDump` ou
   `./gradlew :engine:core:apiDump`;
5. revisar o diff textual da baseline e executar `clean test`.

`apiDump` não é executado automaticamente pela CI. Apagar ou regenerar a
baseline apenas para esconder uma incompatibilidade viola a ADR-003.

## Fronteiras internas e dependências

`verifyPublicApiBoundaries` usa `jdeps --api-only` do JDK 21. Toda dependência
visível em assinatura deve pertencer ao JDK ou a `engine.api.*`; referências a
`engine.internal.*`, `engine.incubator.*`, pacotes legados ou bibliotecas
externas falham.

No Gradle:

- uma dependência necessária apenas à implementação usa `implementation`;
- uma dependência exposta em assinatura pública exige `api`, análise de
  compatibilidade e entrada aprovada em
  [`gradle/public-api-dependencies.txt`](../gradle/public-api-dependencies.txt);
- o catálogo está vazio nesta baseline, portanto `engine:core` não exporta
  dependência transitiva.

O uso conjunto de `jdeps` e do catálogo impede que uma dependência marcada
incorretamente ou um tipo interno passe silenciosamente ao classpath contratual
do consumidor.

## Identidade e eventos incubadores por mundo

A Issue #19 adiciona os contratos experimentais `engine.incubator.world.id.*`
e `engine.incubator.events.*`. Eles não ampliam a baseline `engine.api.*` e
permanecem sujeitos à política de migração de APIs incubadoras.

Cada `WorldState` recebe um `IdGenerator` próprio. O default
`SequentialIdGenerator` produz valores positivos em ordem crescente, retorna
`Long.MAX_VALUE` uma única vez e depois falha com
`EntityIdExhaustedException`, sem wrap. `GameRuntime` recebe uma factory
injetável e solicita um gerador novo por execução; assim, mundos independentes
podem reproduzir a mesma sequência sem compartilhar identidade global. O
registro rejeita qualquer colisão produzida por um gerador customizado antes
de alterar o estado do mundo. IDs emitidos nunca são reutilizados dentro do
mesmo mundo, mesmo após remoção da entidade ou unload do owner.
`WorldState.register(owner, entity)` retorna o
novo ID; o método `add(owner, entity)` preserva seu retorno fluente anterior e
o ID correspondente pode ser consultado por `idOf(entity)`.

Cada mundo também possui exatamente um `WorldEventBus`. Publicação exige
`EventType<T>` explícito, owner pertencente ao mundo e uma destas filas:
`BEFORE_FIXED_UPDATE`, `AFTER_FIXED_UPDATE`, `BEFORE_RENDER` ou
`AFTER_RENDER`. O host escolhe explicitamente a fronteira chamando
`dispatch(phase)`; drenar uma fase nunca drena outra.

A ordem determinística é normativa:

1. eventos da mesma fase são entregues em FIFO;
2. para cada evento, handlers ativos rodam na ordem de assinatura;
3. uma assinatura criada durante um handler começa no evento seguinte;
4. um handle cancelado antes de seu turno não é chamado;
5. eventos publicados durante dispatch entram no fim da fila da fase;
6. dispatch recursivo é rejeitado, evitando interleaving da pilha de handlers.

`EventSubscription.unsubscribe()`/`close()` são idempotentes e seguros durante
dispatch. O unload de um owner remove imediatamente seus handles e eventos
ainda enfileirados. O fechamento do mundo invalida todos os handles e limpa
todas as fases; um restart cria bus e gerador novos. Esses pacotes dependem
somente do JDK e de contratos backend-neutral de `engine:core`.

Migração do contrato lifecycle anterior: substitua
`RuntimeEventQueue.post(owner, payload)` por
`WorldEventBus.post(owner, phase, eventType, payload)` e substitua polling
manual por `dispatch(phase)` com handlers tipados. `GameContext.events()`
continua sendo o ponto de acesso ao bus pertencente ao mundo atual.

## Conteúdo dos JARs

`inspectJars` valida os cinco artifacts atuais:

- `engine:core` é o único que contém `engine.api.*`;
- `engine:gdx` contém somente as implementações incubadoras isoladas de spike,
  input e runtime, além dos recursos internos do spike da Issue #14;
- o JAR principal de `desktop` contém somente o launcher incubador LWJGL3 e o
  classifier `legacy` não duplica a API estável;
- `game` não empacota classes `engine.*`;
- todos carregam `Implementation-Version`, `LICENSE`,
  `THIRD_PARTY_NOTICES.md` e `ASSET_ATTRIBUTION.md` em `META-INF`.

libGDX e LWJGL3 são dependências de implementação do spike e não aparecem em
assinaturas de `engine.api.*`; portanto, a experiência não amplia a superfície
SemVer nem o classpath contratual de `engine:core`.

O task raiz `verifyDistribution` agrega baseline, fronteiras, licenças, assets e
JARs e também inspeciona o ZIP, o backend, o inventário exato de natives
Windows/Linux, a ausência de payload proibido e os textos completos de licença
distribuídos. Ele faz parte de `clean test`, o gate executado pela CI desktop.

## Configuração, logging e métricas incubadores

A Issue #20 adiciona `engine.incubator.runtime.config`,
`engine.incubator.runtime.logging` e `engine.incubator.runtime.metrics`. Esses
pacotes não ampliam `engine.api.*` e podem evoluir conforme a política de APIs
incubadoras.

`EngineConfigLoader` resolve defaults, arquivo e CLI nessa ordem, registra a
origem vencedora de cada campo e rejeita configuração inválida antes de criar o
backend. O application home é uma entrada absoluta; o arquivo padrão fica em
`config/engine.properties` dentro da distribuição e paths relativos usam essa
raiz em vez do CWD.

`EngineLogger` é imutável, recebe clock e sink e cria filhos com `LogContext`
sem estado global. `frame`/`tick` são não negativos e `world` é o execution ID
positivo do `GameContext` quando há um mundo aplicável. `LogFormatter` limita o
formato a campos conhecidos e mantém cada evento em uma linha.

`FrameMetricsCollector` recebe `NanoClock`, `SchedulerMetrics`, saúde de assets
e draw calls para produzir `FrameHealthMetrics`. A implementação libGDX lê esse
snapshot; ela não é fonte paralela de contadores nem autoridade para alterar a
simulação.
