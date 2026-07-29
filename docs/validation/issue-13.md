# Evidências de validação — Issue #13

- Data: 2026-07-28
- Branch: `codex/issue-13-license-semver-api-baseline`
- Base consumida: `43b7571` (`main` após as Issues #9, #10, #11 e #12)
- Ambiente local: Windows 11 amd64
- Build: Gradle Wrapper 9.6.1
- Toolchains: JDK 21 local e Temurin 25.0.4+7 temporário
- Issue: [#13](https://github.com/Balehlah/Engine_lite/issues/13)

## Critérios de aceite

- [x] **GitHub identifica a licença.** Na branch remota, o GitHub expõe o
  `LICENSE` raiz pela navegação `License`. O conteúdo publicado corresponde
  exatamente ao texto `Apache-2.0` retornado pela API canônica de licenças do
  GitHub, com o mesmo SHA-256 após normalização de quebras de linha.
- [x] **Dependências e assets têm origem/licença.**
  `generateDependencyLicenseReport` resolveu 9 módulos, validou 6 entradas de
  tooling/CI e falha se os catálogos divergirem. `verifyAssetAttribution`
  confirmou que não há asset distribuível sem inventário.
- [x] **A baseline detecta quebra de assinatura estável.** Uma renomeação
  temporária de `EngineVersion.current()` fez `apiCheck` falhar na primeira
  linha divergente com exit code 1; a assinatura foi restaurada e o gate final
  passou.
- [x] **API interna não vaza transitivamente.** `jdeps --api-only` encontrou
  apenas `java.lang.Object@java.base` e `java.lang.String@java.base`; a
  configuração Gradle `api` de `engine:core` não expõe dependência.

## Evidências obrigatórias

| Evidência | Resultado | Arquivo |
|---|---|---|
| Identificação da licença | `Apache-2.0`; conteúdo remoto idêntico ao canônico do GitHub | [github-license-detection.json](issue-13/github-license-detection.json) |
| Relatório de licenças | 9 módulos resolvidos; Wrapper, toolchain e 4 Actions registrados | [dependency-license-report.csv](issue-13/dependency-license-report.csv) |
| Falha controlada da baseline | Renome de `current()` rejeitado; exit code 1 | [controlled-api-break.log](issue-13/controlled-api-break.log) |
| Fronteira da API | Somente tipos do JDK; nenhuma dependência `api` | [api-boundaries.txt](issue-13/api-boundaries.txt) |
| Inspeção dos JARs | 5 JARs; versão/notices presentes; boundaries preservados | [jar-inspection.txt](issue-13/jar-inspection.txt) |
| Gate Java 21 | `clean test`; 27 tasks executados; `BUILD SUCCESSFUL` | [clean-test-windows.log](issue-13/clean-test-windows.log) |
| Compatibilidade Java 25 | Reexecução sem cache; 10 tasks executados; `BUILD SUCCESSFUL` | [java25-compatibility.log](issue-13/java25-compatibility.log) |

Os relatórios foram gerados com outputs isolados em
`C:\tmp\engine-lite-issue13-final` porque o OneDrive converteu um `.class` local
em placeholder durante uma tentativa incremental. A propriedade
`isolatedBuildRoot`, já existente, preservou o mesmo build e eliminou a
interferência do filesystem sincronizado.

## Contratos implementados

- `engineVersion=1.0.0-SNAPSHOT` é a versão central e alimenta o manifest de
  todos os JARs.
- `engine.api.*` é a única superfície estável; a baseline inicial contém
  `EngineVersion`.
- `javap -protected -s` protege assinaturas públicas e protegidas de tipos
  acessíveis, sem estabilizar classes privadas.
- `jdeps --api-only` rejeita tipos internos, incubadores, legados ou externos
  em assinaturas estáveis.
- Dependências expostas com Gradle `api` exigem registro explícito; o catálogo
  atual está vazio.
- Todos os JARs incluem licença, notices e atribuição de assets em `META-INF`.
- `clean test` agrega `verifyDistribution`, portanto os gates são executados
  pela matriz Windows/Linux/macOS existente.

## Comandos finais

```text
gradlew.bat --no-daemon --console=plain --rerun-tasks clean test \
  -PtestRandomSeed=1313 \
  -PisolatedBuildRoot=C:/tmp/engine-lite-issue13-final

gradlew.bat --no-daemon --console=plain --rerun-tasks \
  java25CompatibilityTest \
  -PtestRandomSeed=1325 \
  -PisolatedBuildRoot=C:/tmp/engine-lite-issue13-final

git diff --check
```

## Validação remota

- PR: [#58](https://github.com/Balehlah/Engine_lite/pull/58), publicada sem
  merge na `main`.
- CI inicial:
  [run 30421278941](https://github.com/Balehlah/Engine_lite/actions/runs/30421278941),
  concluído com sucesso em Ubuntu, Windows e macOS; em cada sistema passaram
  `clean test` no Java 21 e o smoke de compatibilidade no Java 25.
- Licença: o arquivo remoto e o texto canônico `apache-2.0` da API do GitHub
  produziram o SHA-256 normalizado
  `43070e2d4e532684de521b885f385d0841030efa2b1a20bafb76133a5e1379c1`.
- A detecção no cabeçalho da página padrão do repositório ocorrerá quando o PR
  for integrado, pois o endpoint de licença do repositório avalia a branch
  padrão. Nenhum merge foi realizado, conforme a política de contribuição.

## Risco residual e rollback

A baseline é preparatória até o primeiro RC; atualizá-la cedo demais pode
congelar uma API imatura. Por isso apenas `EngineVersion` foi classificada e os
pacotes legados permanecem fora do contrato.

Reverter esta branch remove licença, versão, API estável e gates sem alterar o
comportamento do protótipo. Uma baseline já publicada nunca deve ser apagada
para ocultar incompatibilidade; qualquer substituição de licença exige nova ADR
e revisão das contribuições.
