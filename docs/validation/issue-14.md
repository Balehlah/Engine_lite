# Evidências de validação — Issue #14

- Data de início: 2026-07-29
- Branch: `codex/issue-14-libgdx-lwjgl3-spike`
- Base consumida: `9de87d9`
- Issue: [#14](https://github.com/Balehlah/Engine_lite/issues/14)
- Estado: gates, integração, revisão independente e decisão ADR-002 concluídos;
  Issue #60 fechada e PR #59 pronta para revisão

## Gate de entrada

- [x] #10 fechada em 2026-07-25.
- [x] #12 fechada em 2026-07-27.
- [x] #13 fechada em 2026-07-29.
- [x] #60 integrada ao branch do spike pelo merge `628bfb3`.
- [x] Baseline local anterior à implementação:
  `gradlew.bat --no-daemon clean test -PtestRandomSeed=1414
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue14-baseline`.

O baseline terminou com `BUILD SUCCESSFUL`: 29 tasks acionáveis, 18 executadas,
5 recuperadas do cache e 6 atualizadas. A alteração staged de `.gitignore` no
checkout principal foi preservada fora desta worktree.

## Checklist técnico

- [x] `clean test` final no Java 21.
- [x] testes determinísticos da escala inteira e descarte.
- [x] compatibilidade Java 25.
- [x] distribuição reproduzível.
- [x] execução do pacote a partir de CWD externo.
- [x] smoke gráfico local autoencerrável.
- [x] três PNGs com as fixtures da ADR-002.
- [x] logs de lifecycle, input, asset, áudio, Tiled e dispose.
- [x] matriz Windows/Linux verde em Java 21/25.
- [x] hashes dos artifacts locais.
- [x] revisão independente de `qa_validator`.
- [x] aprovação final de @Balehlah.

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
instalou Temurin 21 e 25 explicitamente e fechou o item com execução real, sem
inferir compatibilidade a partir do Java 21.

## Evidências remotas

A implementação da #60 foi publicada na PR empilhada
[#61](https://github.com/Balehlah/Engine_lite/pull/61). A execução
[`30554662980`](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980)
aprovou os jobs
[Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/job/90911919800)
e
[Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/job/90911919770)
em Java 21/25.

Cada job construiu a distribuição e iniciou o mesmo pacote instalado a partir
de CWD externo nos dois JDKs, em vez de executar o classpath do Gradle. Os
artifacts de distribuição/evidências estão preservados para
[Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/artifacts/8764391986)
e
[Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/artifacts/8764399932).
Os relatórios de teste estão preservados para
[Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/artifacts/8764390426)
e
[Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30554662980/artifacts/8764399139).

Os quatro smokes registraram lifecycle, três viewports/goldens, input, assets,
OpenAL, Tiled e descarte exato de nove recursos. O mesmo ZIP foi produzido nos
dois runners e permaneceu intacto antes/depois de todos os smokes:
`9f9b53677c975233ebc72ad3d4f457e670e4f05419c6f6e749eff86a193f8d15`.
No Windows, Java 21 e 25 reportaram
`llvmpipe (LLVM 22.1.6, 256 bits)` com Mesa 26.1.1 fixado e auditado.

O `qa_validator` independente confrontou a matriz, os quatro artifacts, 14
entradas de manifesto por smoke, estrutura interna do ZIP/JARs, notices e
proveniência. Também repetiu o gate local completo com seed `6061`. O parecer
foi **PASS, zero defeitos bloqueadores**.

Após a integração da PR #61, a PR #59 reexecutou a matriz na
[execução 30559042291](https://github.com/Balehlah/Engine_lite/actions/runs/30559042291).
Os jobs
[Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30559042291/job/90926973949)
e
[Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30559042291/job/90926973734)
passaram novamente.

## Decisão ADR-002

Em 2026-07-30, depois da matriz completa e do parecer QA sem bloqueadores,
@Balehlah aprovou **aceitar libGDX/LWJGL3 como backend desktop**, preservando
Java2D como fallback legado. ADR-002 passa ao estado aceita e D-011 registra a
decisão final sem reescrever a D-008 histórica. Migração horizontal e remoção
do fallback permanecem fora do escopo desta entrega.

A execução histórica
[`30476609058`](https://github.com/Balehlah/Engine_lite/actions/runs/30476609058)
aprovou Ubuntu e falhou antes da aplicação no runner Windows por ausência de WGL.
A Issue #60 substitui o contrato de plataforma e provisiona Mesa llvmpipe
auditável para esse runner; a execução histórica não é tratada como verde.

## Defeitos, risco residual e rollback

Defeitos encontrados durante a implementação e corrigidos antes desta coleta:
input sintético que contornava o backend, golden parcial, orientação vertical
do sprite, conversão HiDPI, execução do classpath em vez do ZIP, limite de
classpath do launcher Windows e exclusão incorreta dos decoders JLayer/JOrbis.

Riscos residuais não bloqueadores:

- a evidência OpenAL com driver `null` não prova saída audível;
- a matemática HiDPI 1×/1,25×/2× permanece coberta por teste determinístico;
- uma falha do filesystem enquanto o logger de `dispose` escreve evidência pode
  interromper o restante do relatório; o caminho verde libera todos os recursos.

O rollback previsto para a #60 reverte sua PR empilhada sem tocar em Java2D. O
rollback do spike remove somente `engine:gdx`, launcher LWJGL3, assets,
dependências e tasks experimentais; o backend Java2D transitório e
`engine:core` permanecem intactos.
