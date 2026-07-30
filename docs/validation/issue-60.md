# Evidências de validação — Issue #60

- Data de início: 2026-07-30
- Branch: `codex/issue-60-windows-linux-mesa`
- Base consumida: `e101bcc7d8e1151557078a1305b7ea3ba9cb052b`
- Base remota:
  [PR #59](https://github.com/Balehlah/Engine_lite/pull/59)
- Issue: [#60](https://github.com/Balehlah/Engine_lite/issues/60)
- Estado: implementação e gates técnicos concluídos; integração e aprovações
  pendentes

## Baseline anterior à mudança

Worktree limpa criada diretamente do head vigente da PR #59. O checkout `main`
e a worktree da Issue #14 não foram alterados.

```text
gradlew.bat --no-daemon --stacktrace clean test
  buildSpikeDistribution verifyDistribution
  -PtestRandomSeed=6060
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue60-baseline
```

Resultado confirmado: `BUILD SUCCESSFUL`, 38 tasks acionáveis, 24 executadas,
8 recuperadas do cache e 6 atualizadas.

O smoke Windows Java 21 baseline executou o ZIP extraído a partir de CWD externo
e terminou com exit 0. SHA-256 antes/depois:

```text
113ed4e89538ed2c41361e1f2fe6c5af8a40fcbbc10ca2238bac4f6d44f6d562
```

A execução remota histórica
[`30476609058`](https://github.com/Balehlah/Engine_lite/actions/runs/30476609058)
aprovou build, Java 21/25, pacote e os dois smokes no Ubuntu. O smoke Windows
falhou antes da aplicação com `GLFW_API_UNAVAILABLE` e
`WGL: The driver does not appear to support OpenGL`.

## Contrato de plataforma

- [x] ADR-006 preserva ADR-001/D-002/D-009 como histórico.
- [x] D-010 registra Windows/Linux como únicas famílias suportadas.
- [x] ADR-002 mede somente a matriz suportada e preserva fallback.
- [x] Documentos normativos atuais reconciliados.
- [x] Evidências fechadas permanecem históricas.

## Mesa Windows CI

| Campo | Valor |
|---|---|
| Versão | 26.1.1 |
| Origem | `pal1000/mesa-dist-win` |
| Tag/commit | `1e2b696ce9e81e77e17ee6e4787587237ce9d2ed` |
| Arquivo | `mesa3d-26.1.1-release-msvc.7z` |
| SHA-256 arquivo | `d5e90e9ae4d620313b61fbbf8e9a55761454e38b6501c39be6d93449c88780e1` |
| SHA-256 `opengl32.dll` | `d2645f47b4dee4f47dcdfc1b2021a70f471655d95a019cfd1fb48415810867ed` |
| SHA-256 `libgallium_wgl.dll` | `27f16f9e98119ad529ed915d4f65c3a2e8d84b4f8cbdce2f13cda0637b73e05c` |
| Licença | core MIT; licenças por arquivo identificadas por SPDX |

O script exige `RUNNER_TOOL_CACHE`, recusa sobrescrita, verifica o archive antes
da extração, extrai somente dois DLLs, verifica ambos, consulta `KnownDLLs` e
copia somente para os `bin` dos JDKs 21/25 efêmeros. A CI executa prova negativa
com checksum deliberadamente incorreto.

## Distribuição

- [x] libGDX upstream fixado pelo SHA-256
  `f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970`.
- [x] JAR curado contém exatamente os seis payloads Windows/Linux contratados.
- [x] Classifiers LWJGL são allowlisted por módulo e plataforma/arquitetura.
- [x] `verifyDistribution` inspeciona nomes, launchers e conteúdo interno dos
  JARs.
- [x] Tooling Mesa é proibido no pacote.
- [x] Inventário/hash local final do ZIP anexado.

Validação local limpa em Java 21, anterior à normalização final dos assets
textuais no checkout da CI:

```text
gradlew.bat --no-daemon --stacktrace clean test
  buildSpikeDistribution verifyDistribution recordSpikeDistributionHash
  -PtestRandomSeed=6060
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue60-final

BUILD SUCCESSFUL
41 tasks acionáveis: 28 executadas, 7 do cache, 6 atualizadas

gradlew.bat --no-daemon --stacktrace
  smokeSpikeDistribution verifySpikeDistributionHash
  -PspikeSmokeVariant=issue60-final-local-java21
  -PisolatedBuildRoot=C:\tmp\engine-lite-issue60-final

BUILD SUCCESSFUL
SHA-256 local antes/depois:
40e3676528260ecdd14df18e610e17746b9478dac395b5463f1bb66d63392493
```

Inventário verificado:

```text
zip-bytes=13778457
desktop-natives=curated-windows-linux
desktop-natives-source-sha256=f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970
desktop-natives-curated-sha256=1f69de2db33fae4c4f3328ef6fbfffe3a85bce815326e3346362b7cee0376a4f
lwjgl-linux-windows-natives=exact
macos-natives=absent
mesa-runtime=absent
```

## Matriz e evidências remotas

| Gate | Ubuntu Java 21 | Ubuntu Java 25 | Windows Java 21 | Windows Java 25 |
|---|---:|---:|---:|---:|
| Build/test | PASS | PASS | PASS | PASS |
| ZIP/CWD externo | PASS | PASS | PASS | PASS |
| Lifecycle | PASS | PASS | PASS | PASS |
| Viewport/goldens | PASS | PASS | PASS | PASS |
| Input/assets | PASS | PASS | PASS | PASS |
| OpenAL/Tiled | PASS | PASS | PASS | PASS |
| Dispose exato | PASS | PASS | PASS | PASS |
| Renderer exigido | PASS (`llvmpipe`) | PASS (`llvmpipe`) | PASS (`llvmpipe`) | PASS (`llvmpipe`) |

- PR empilhada:
  [#61](https://github.com/Balehlah/Engine_lite/pull/61)
- Execução verde:
  [`30553586242`](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242)
- Jobs:
  [Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/job/90908188967)
  e
  [Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/job/90908188982)
- Artifacts de distribuição e evidências:
  [Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/artifacts/8763917439)
  e
  [Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/artifacts/8763980579)
- Relatórios de testes:
  [Ubuntu](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/artifacts/8763915942)
  e
  [Windows](https://github.com/Balehlah/Engine_lite/actions/runs/30553586242/artifacts/8763979557)
- SHA-256 canônico do mesmo ZIP no Ubuntu e Windows, antes e depois dos
  quatro smokes:
  `9f9b53677c975233ebc72ad3d4f457e670e4f05419c6f6e749eff86a193f8d15`.

Nos dois JDKs, Ubuntu reportou `llvmpipe (LLVM 20.1.2, 256 bits)` e Windows
reportou `llvmpipe (LLVM 22.1.6, 256 bits)`. Os relatórios Windows registraram
origem, commit e três digests Mesa fixados. A prova negativa observou o digest
real do archive, recusou o digest deliberadamente incorreto e registrou
`mesa.checksum.negative=PASS`.

Cada smoke registrou `result=PASS`, três fixtures, CWD externo, OpenAL `null`,
integridade pré/pós, três goldens e nove recursos descartados exatamente uma
vez. O `verifyDistribution` aprovou a allowlist exata Windows/Linux e rejeita
qualquer payload, extensão ou flag fora do contrato, além de runtime Mesa no
ZIP.

## Checklist de fechamento

- [x] `clean test` local final.
- [x] `java25CompatibilityTest` na matriz.
- [x] `buildSpikeDistribution verifyDistribution` final.
- [x] Quatro smokes remotos do mesmo ZIP.
- [x] Dois checks verdes.
- [ ] Proteção de branch exige exatamente os dois checks suportados.
- [ ] PR integrada ao branch da #14.
- [ ] PR #59 reexecutada verde após integração.
- [ ] Revisão independente de `qa_validator` sem bloqueadores.
- [ ] Aprovação final de @Balehlah.

## Risco residual e rollback

O driver OpenAL `null` não prova saída audível. Mesa introduz risco de supply
chain limitado pelo pin de origem, allowlist, hashes e ambiente descartável.
Reverter integralmente a PR empilhada restaura o branch da #14 sem apagar
evidências, remover Java2D ou declarar suporte adicional.
