# Evidências de validação — Issue #14

- Data de início: 2026-07-29
- Branch: `codex/issue-14-libgdx-lwjgl3-spike`
- Base consumida: `9de87d9`
- Issue: [#14](https://github.com/Balehlah/Engine_lite/issues/14)
- Estado: gates locais concluídos; matriz remota e decisão final pendentes

## Gate de entrada

- [x] #10 fechada em 2026-07-25.
- [x] #12 fechada em 2026-07-27.
- [x] #13 fechada em 2026-07-29.
- [x] Baseline local anterior à implementação:
  `gradlew.bat --no-daemon clean test -PtestRandomSeed=1414
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue14-baseline`.

O baseline terminou com `BUILD SUCCESSFUL`: 29 tasks acionáveis, 18 executadas,
5 recuperadas do cache e 6 atualizadas. A alteração staged de `.gitignore` no
checkout principal foi preservada fora desta worktree.

## Checklist técnico

- [x] `clean test` final no Java 21.
- [x] testes determinísticos da escala inteira e descarte.
- [ ] compatibilidade Java 25.
- [x] distribuição reproduzível.
- [x] execução do pacote a partir de CWD externo.
- [x] smoke gráfico local autoencerrável.
- [x] três PNGs com as fixtures da ADR-002.
- [x] logs de lifecycle, input, asset, áudio, Tiled e dispose.
- [ ] matriz Windows/Linux/macOS verde.
- [x] hashes dos artifacts locais.
- [ ] revisão independente de `qa_validator`.
- [ ] aprovação final de @Balehlah.

## Evidências locais

### Gate limpo

```text
gradlew.bat --no-daemon --stacktrace clean test
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue14-full3
  -PtestRandomSeed=1414
```

Resultado: `BUILD SUCCESSFUL` em 55 s; 38 tasks acionáveis, 25 executadas,
7 recuperadas do cache e 6 atualizadas. O relatório de licenças registrou
21 módulos resolvidos e 6 entradas de tooling. As suítes de `engine:core`,
`engine:gdx` e `desktop` somaram 54 testes, sem falha ou erro, e 12 skips
esperados de caracterização. Há 19 testes específicos do spike sem skip:
16 em `engine:gdx` para viewport, HiDPI lógico, políticas gráficas,
configuração, assets textuais e descarte, mais 3 para o launcher em `desktop`.

### Pacote executado

```text
gradlew.bat --no-daemon --stacktrace
  :desktop:generateSpikeEvidenceManifest
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue14-full3
  -PspikeSmokeVariant=windows-java21-v7
  -PtestRandomSeed=1414
```

Resultado: `BUILD SUCCESSFUL`; o task extraiu
`engine-lite-spike-1.0.0-SNAPSHOT.zip`, iniciou seu `.bat` em um CWD temporário
fora do repositório e registrou 14 hashes. O ZIP possui 16.393.699 bytes.

| Evidência | Resultado local |
|---|---|
| Lifecycle | `create`, callbacks de resize 640×360/800×600/1280×720, `pause` e `dispose` sem exceção |
| Viewport | 2× sem barras; 2× com barras 80/120; 4× sem barras; golden integral `PASS` |
| Input/sprite | quatro eventos; callback `mouseMoved(17,29)` do backend e deslocamento do sprite `PASS` |
| Assets | sprite interno e TMX carregados com SHA-256 a partir do CWD externo |
| Áudio | `OpenALLwjgl3Audio`, OpenAL Soft 1.25.1, backend `null`, source id 0 |
| Recursos | 9 recursos possuídos; cada contador final é 1 |
| Packaging | ZIP extraído e launcher executado com Java 21.0.3 |

SHA-256 do ZIP:
`101ba840cebe7e359457d88ab473a8f2544fa639d05644d193149ee702f92887`.
Uma segunda build em `C:\tmp\engine-lite-issue14-repro` produziu o mesmo hash.

A tentativa local de `java25CompatibilityTest` não iniciou porque esta máquina
não possui JDK 25 e o build não habilita download implícito de toolchains. A CI
instala Temurin 21 e 25 explicitamente; por isso o item permanece aberto e não
é inferido a partir do Java 21.

## Evidências remotas

Os links do pull request, da execução da CI, dos três jobs e dos artifacts serão
registrados após a publicação da branch. Cada job deve construir a distribuição
e iniciar o pacote instalado, não apenas executar o classpath do Gradle.

## Defeitos, risco residual e rollback

Defeitos encontrados durante a implementação e corrigidos antes desta coleta:
input sintético que contornava o backend, golden parcial, orientação vertical
do sprite, conversão HiDPI, execução do classpath em vez do ZIP, limite de
classpath do launcher Windows e exclusão incorreta dos decoders JLayer/JOrbis.

Riscos residuais não bloqueadores:

- a evidência OpenAL com driver `null` não prova saída audível;
- a matemática HiDPI 1×/1,25×/2× tem teste determinístico, mas Retina real será
  observado no runner macOS;
- uma falha do filesystem enquanto o logger de `dispose` escreve evidência pode
  interromper o restante do relatório; o caminho verde libera todos os recursos.

O rollback previsto remove somente `engine:gdx`, o launcher LWJGL3, assets,
dependências e tasks do spike; o backend Java2D transitório e `engine:core`
permanecem intactos.
