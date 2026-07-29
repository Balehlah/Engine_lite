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

## Conteúdo dos JARs

`inspectJars` valida os cinco artifacts atuais:

- `engine:core` é o único que contém `engine.api.*`;
- `engine:gdx` continua vazio até a Issue #14;
- o JAR principal de `desktop` continua vazio e o classifier `legacy` não
  duplica a API estável;
- `game` não empacota classes `engine.*`;
- todos carregam `Implementation-Version`, `LICENSE`,
  `THIRD_PARTY_NOTICES.md` e `ASSET_ATTRIBUTION.md` em `META-INF`.

O task raiz `verifyDistribution` agrega baseline, fronteiras, licenças, assets e
JARs. Ele faz parte de `clean test`, o gate executado pela CI desktop.
