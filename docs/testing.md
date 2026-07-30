# Estratégia de testes

O build usa JUnit Jupiter 5.14.4 sobre a JUnit Platform. O Gradle Wrapper é o
único entrypoint suportado, usa Java 21, força modo AWT headless nos testes e
publica relatórios JUnit XML e HTML.

## Categorias

- `specification`: comportamento pretendido e protegido. Algumas specifications
  ficam `@Disabled` enquanto um defeito conhecido ainda depende de uma issue
  futura.
- `characterization`: reprodução executável do comportamento atual, inclusive
  quando ele é defeituoso. Uma caracterização não transforma o bug em API; ela
  deve ser substituída pela specification correspondente na mesma mudança que
  corrigir a produção.

Toda specification desabilitada inclui o ID do achado e a issue sucessora
quando ela já existe. Não remova uma caracterização apenas para deixar o build
verde.

## Execução

Suíte completa com ordem pseudoaleatória explícita:

```batch
gradlew.bat --no-daemon clean test -PtestRandomSeed=1101
gradlew.bat --no-daemon clean test -PtestRandomSeed=1129
```

No Linux, substitua `gradlew.bat` por `./gradlew`.

Filtros opcionais por tag:

```batch
gradlew.bat test -PincludeTags=characterization
gradlew.bat test -PincludeTags=specification
gradlew.bat test -PexcludeTags=characterization
```

`testRandomSeed` controla os orderers aleatórios globais de classes e métodos.
Uma suíte saudável deve passar com qualquer seed e sem depender da ordem.

Worktrees sob sincronizadores que bloqueiam outputs podem apontar somente os
diretórios descartáveis de build para fora da árvore:

```batch
gradlew.bat clean test -PisolatedBuildRoot=C:\tmp\engine-lite-build
```

Essa propriedade não muda fontes nem o default `<projeto>/build`; todos os
gates calculam os caminhos pela propriedade Gradle `layout.buildDirectory`.

## Relatórios publicados

Cada módulo testado publica:

- XML compatível com JUnit em `<módulo>/build/test-results/test/`;
- HTML em `<módulo>/build/reports/tests/test/index.html`.

O task `aggregateTestReport`, finalizador do `test` da raiz, publica a visão HTML
consolidada em `build/reports/tests/aggregate/index.html`. O gate
`verifyJUnitReports` falha quando os relatórios obrigatórios de `engine:core` ou
`desktop` não existem. Uma CI futura pode fazer upload desses mesmos diretórios
sem mudar o contrato local.

## Isolamento

Testes preferem instâncias novas. Quando o singleton legado `Input` precisa ser
exercitado, `@BeforeEach` e `@AfterEach` limpam seu estado e `@ResourceLock`
impede concorrência sobre o global. Fixtures de arquivo usam `@TempDir` e são
removidas pelo JUnit.

## Evidências da Issue #11

A matriz de rastreabilidade, as duas seeds e a mutação controlada ficam em
[`validation/issue-11.md`](validation/issue-11.md).
