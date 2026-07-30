# Integração contínua desktop

O workflow [`build.yml`](../.github/workflows/build.yml) é o gate de build,
testes, pacote e smoke para pull requests, merge queue e pushes em `main`.
Conforme D-010/ADR-006, ele produz somente dois checks independentes:

- `Build and test (Ubuntu)`;
- `Build and test (Windows)`.

A matriz usa runners x64 nativos (`ubuntu-24.04` e `windows-2025`),
`fail-fast: false` e timeout de 30 minutos por sistema. Uma falha não oculta o
resultado nem as evidências do outro sistema.

## Contrato do gate

Cada check:

1. instala Temurin 21 e 25;
2. valida o `gradle-wrapper.jar` pelo `gradle/actions/setup-gradle`;
3. registra sistema, arquitetura, imagem, revisão, JDK e Wrapper;
4. executa `clean test` com Java 21 e seed própria;
5. executa `java25CompatibilityTest` mantendo bytecode Java 21;
6. valida API, licenças, assets, JARs e distribuição;
7. constrói o ZIP canônico e registra seu SHA-256 antes dos smokes;
8. extrai e executa esse ZIP a partir de CWD externo em Java 21 e 25;
9. valida as três capturas de viewport, OpenAL Soft `null`, lifecycle, input,
   assets, Tiled, descarte e manifestos SHA-256;
10. confirma que o SHA-256 do ZIP continua idêntico depois dos dois smokes;
11. publica relatórios e pacote/evidências em artifacts por sistema.

Ubuntu executa o pacote sob Xvfb/Mesa. Windows provisiona Mesa llvmpipe conforme
o contrato abaixo. O workflow chama exclusivamente `gradlew`/`gradlew.bat`.

## Mesa llvmpipe no runner Windows

[`provision-mesa-windows.ps1`](../.github/scripts/provision-mesa-windows.ps1)
é tooling de CI, não parte do produto.

| Item | Valor fixado |
|---|---|
| Distribuição | `pal1000/mesa-dist-win` 26.1.1 |
| Tag/commit | `1e2b696ce9e81e77e17ee6e4787587237ce9d2ed` |
| URL | `https://github.com/pal1000/mesa-dist-win/releases/download/26.1.1/mesa3d-26.1.1-release-msvc.7z` |
| SHA-256 do arquivo | `d5e90e9ae4d620313b61fbbf8e9a55761454e38b6501c39be6d93449c88780e1` |
| Entrada extraída | `x64/opengl32.dll` |
| SHA-256 de `opengl32.dll` | `d2645f47b4dee4f47dcdfc1b2021a70f471655d95a019cfd1fb48415810867ed` |
| Entrada extraída | `x64/libgallium_wgl.dll` |
| SHA-256 de `libgallium_wgl.dll` | `27f16f9e98119ad529ed915d4f65c3a2e8d84b4f8cbdce2f13cda0637b73e05c` |
| Licença | core MIT; cada arquivo conserva seu SPDX aplicável |

O script:

- exige archive, extração e JDKs dentro de `RUNNER_TOOL_CACHE`;
- recusa sobrescrita;
- verifica o arquivo antes de extrair;
- extrai somente as duas entradas allowlisted e verifica ambos os hashes;
- consulta `KnownDLLs` antes da cópia;
- copia os DLLs apenas para `%JAVA_HOME%\bin` e
  `%JAVA_HOME_25_X64%\bin`;
- publica versão, origem, commit, licença e digests para
  `runner.properties`.

Uma etapa negativa chama o mesmo verificador com um SHA-256 deliberadamente
incorreto. O job só continua quando essa chamada falha e grava
`mesa.checksum.negative=PASS`.

Os smokes Windows executam com `GALLIUM_DRIVER=llvmpipe` e
`LIBGL_ALWAYS_SOFTWARE=1`. `probe.log` deve registrar `gl.renderer` contendo
`llvmpipe`; ausência ou renderer diferente falha o check.

O arquivo baixado, a extração, os JDKs modificados e os DLLs Mesa nunca entram
no ZIP, classpath distribuído ou artifacts. `verifyDistribution` inspeciona
nomes e conteúdo dos JARs para impedir regressão.

## Distribuição e evidências

O ZIP inclui somente natives contratados de Windows/Linux. O artifact upstream
`gdx-platform-1.14.2-natives-desktop.jar` é verificado pelo SHA-256
`f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970`
e convertido de forma reproduzível em
`gdx-platform-1.14.2-natives-windows-linux.jar`. O build exige a entrada
allowlisted, licença Apache-2.0 e proveniência em `third_party/`.

O artifact `test-reports-<SO>` contém XML/HTML JUnit, visão agregada, licenças,
inspeção de JARs e baseline de API.

O artifact `spike-distribution-and-evidence-<SO>` contém:

- `desktop/build/distributions/*.zip`;
- `desktop/build/reports/spike/**`, incluindo Java 21/25 e manifestos;
- `build/reports/distribution/**`, incluindo inventário, hash pré/pós-smoke e
  prova negativa de checksum no Windows.

`if-no-files-found: error` transforma ausência de evidência em falha explícita.

## Cache

O cache Gradle usa provider `basic`, Wrapper validado e chaves por sistema e
conteúdo. Pull requests e merge queue são somente leitura; apenas pushes em
`main` podem gravar. Ubuntu repete `clean test` para registrar prova local de
miss/hit.

## Proteção de `main`

O ruleset de `main` deve exigir:

- pull request antes do merge;
- `Build and test (Ubuntu)`;
- `Build and test (Windows)`;
- branch atualizada antes do merge, quando essa política estiver habilitada;
- nenhum bypass para automações comuns.

Os dois nomes são estáveis e o workflow não usa filtro de paths. Se um check
legado da matriz histórica ainda estiver configurado, ele deve ser removido do
ruleset na mesma entrega da Issue #60.

## Rollback

Reverter a PR da Issue #60 restaura workflow, distribuição e documentação
anteriores sem alterar Java2D. Uma indisponibilidade do Mesa fixado não autoriza
remover silenciosamente o smoke ou aceitar outro renderer: registre o bloqueio,
revise origem/hashes por nova mudança aprovada e mantenha o check vermelho.
