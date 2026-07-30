# Evidências de validação — Issue #10

> Registro histórico fechado. Referências à matriz vigente na data desta
> validação foram substituídas por D-010/ADR-006.

- Data: 2026-07-25
- Branch: `codex/issue-10-gradle-modules`
- Base consumida: `645efcf` (entrega fechada da Issue #9)
- Ambiente local: Windows 11 amd64, Temurin 21.0.3 LTS
- Build: Gradle Wrapper 9.6.1 com SHA-256 fixado
- Issue: [#10](https://github.com/Balehlah/Engine_lite/issues/10)

## Critérios de aceite

- [x] Clone limpo executa `clean test` pelos entrypoints Windows e Unix/POSIX.
  Evidência: um snapshot temporário foi commitado com `gradlew`, `build.sh` e
  `run.sh` em modo `100755`, clonado localmente sem `bin/` ou diretórios de
  build e validado com `gradlew.bat --no-daemon clean test` e
  `./gradlew --no-daemon clean test`. Ambos terminaram com exit code 0.
- [x] O build não lê `bin/`.
  Evidência: o clone validado não continha `bin/`, e `verifyNoBinInput` inspeciona
  todos os source sets e falha se algum deles apontar para esse diretório.
- [x] `engine:core` não aceita libGDX/AWT.
  Evidência: `verifyBackendIndependence` verifica referências Java e dependências
  declaradas contra AWT, Swing, libGDX e LWJGL. O gate passou sobre as quatro
  fontes neutras atuais: `Timer`, `Vector2`, `Logger` e `RandomUtils`.
- [x] A demo legada possui task transitória documentada.
  Evidência: `legacyDemo` e `legacyDemoSmoke` estão documentadas no README; o
  smoke abriu a janela Java2D, inicializou engine/input/cena, encerrou os
  recursos e emitiu `LEGACY_DEMO_SMOKE_OK`.
- [x] O root não publica JAR monolítico.
  Evidência: somente o plugin `base` é aplicado à raiz e
  `verifyRootDoesNotPublishJar` confirmou que não existe task `:jar`.

## Topologia dos módulos

```text
Root project 'engine-lite'
+--- Project ':desktop' - Desktop boundary and transitional Java2D backend.
+--- Project ':engine'
|    +--- Project ':engine:core' - Backend-neutral Engine Lite core.
|    \--- Project ':engine:gdx' - Reserved boundary for the Issue #14 spike.
\--- Project ':game' - Reference game consuming the transitional backend.
```

O backend Java2D restante é compilado no source set explícito
`:desktop:legacy`. `:game` consome seu artifact transitório; `:engine:gdx`
permanece sem libGDX até o spike da Issue #14.

A toolchain, o bytecode (`--release 21`), a compilação/Javadoc e os subprocessos
Java estão fixados em Java 21/UTF-8. O smoke final confirmou caracteres
portugueses íntegros no console.

## Evidências obrigatórias

| Evidência | Resultado | Log |
|---|---|---|
| Clone limpo, Windows | Exit code 0; `BUILD SUCCESSFUL` | [clean-test-windows.log](issue-10/clean-test-windows.log) |
| Clone limpo, Unix/POSIX | Exit code 0; scripts `100755`; `BUILD SUCCESSFUL` | [clean-test-posix.log](issue-10/clean-test-posix.log) |
| `gradle projects` | Quatro módulos contratuais presentes | [projects.log](issue-10/projects.log) |
| Smoke da demo | Inicialização e cleanup; marcador `LEGACY_DEMO_SMOKE_OK` | [legacy-demo-smoke.log](issue-10/legacy-demo-smoke.log) |
| Fontes/classes | 37 fontes; 45 classes antigas; 44 reproduzíveis | [class-comparison.log](issue-10/class-comparison.log) |

## Comparação antes/depois e remoção de `bin/`

Antes da remoção havia 37 fontes Java e 45 `.class` versionados. O build Gradle
com `javac --release 21` reproduz 44 classes. A única diferença é
`engine/graphics/Animation$1.class`, helper sintético de enum/switch deixado por
uma compilação anterior e não emitido pelo compilador atual.

O inventário verificável está em
[`gradle/legacy-class-baseline.txt`](../../gradle/legacy-class-baseline.txt).
Qualquer ausência ou classe nova fora da exceção documentada faz
`verifyLegacyClassParity` falhar. Somente após esse gate passar os 45 binários
versionados foram removidos.

## Comandos executados

```text
gradlew.bat --no-daemon clean test
./gradlew --no-daemon clean test
gradlew.bat --no-daemon projects
gradlew.bat --no-daemon legacyDemoSmoke
git diff --check
```

Todos terminaram com exit code 0 na validação final.

## Risco residual e responsabilidade

- A validação POSIX local exercitou o script Unix pelo Git Bash e comprovou o
  modo `100755`. A matriz nativa Windows/Linux/macOS permanece responsabilidade
  da Issue #12, conforme ADR-001; nenhum workflow de CI foi antecipado aqui.
- O Java2D continua transitório e contém pacotes mistos. A migração horizontal
  permanece bloqueada até o gate da ADR-002/Issue #14.
- `engine:gdx` é somente um boundary; nenhuma dependência libGDX foi adicionada.

A revisão deve ser solicitada aos papéis `devops-release` e `qa_validator`.
Nenhum merge automático faz parte desta entrega.

## Rollback

Reverter os arquivos Gradle, Wrapper, scripts e documentação restaura o build
manual. Os `.class` removidos podem ser recuperados pelo Git, embora não devam
voltar a ser fonte de verdade. As decisões da Issue #9 não são alteradas.
