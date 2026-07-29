# Integração contínua desktop

O workflow [`build.yml`](../.github/workflows/build.yml) é o gate inicial de
build e testes para pull requests, merge queue e pushes em `main`. Ele produz
três checks independentes:

- `Build and test (Ubuntu)`;
- `Build and test (Windows)`;
- `Build and test (macOS)`.

A matriz usa runners x64 nativos (`ubuntu-24.04`, `windows-2025` e
`macos-15-intel`), `fail-fast: false` e timeout de 30 minutos por sistema. Assim,
uma falha não oculta o resultado nem os relatórios dos outros sistemas.

## Contrato do gate

Cada check:

1. instala Temurin 21 e 25;
2. valida o `gradle-wrapper.jar` pelo `gradle/actions/setup-gradle`;
3. registra nos logs o sistema, a arquitetura, a imagem do runner, a revisão
   Git, o JDK ativo e a versão do Gradle Wrapper;
4. executa `clean test` com Java 21 e uma seed própria;
5. executa `java25CompatibilityTest`, que mantém o bytecode em Java 21 e inicia
   os testes em Java 25;
6. valida baseline/API, licenças, assets e conteúdo dos JARs como parte de
   `clean test`;
7. publica XML JUnit, HTML e os relatórios de distribuição em um artifact por
   sistema.

O workflow chama exclusivamente `gradlew`/`gradlew.bat`. Ele não chama `javac`,
não enumera fontes e não usa `build.sh`, `build.bat` ou scripts manuais como
entrada alternativa.

Os testes JUnit e gates de integração do build permanecem em `clean test`. O
smoke de compatibilidade de runtime fica no task separado
`java25CompatibilityTest`, com timeout próprio. O smoke gráfico
`legacyDemoSmoke` não é gate inicial: ele depende de display e permanece
separado até existir uma estratégia nativa estável por sistema, conforme a
exclusão de golden instável da Issue #12.

## Relatórios

O artifact `test-reports-<SO>` é enviado mesmo quando o job falha e fica retido
por 14 dias. Quando a suíte chega à publicação, ele contém:

- `**/build/test-results/test/**` e `**/build/reports/tests/test/**` para Java
  21;
- `**/build/test-results/java25CompatibilityTest/**` e
  `**/build/reports/tests/java25CompatibilityTest/**` para Java 25;
- `build/reports/tests/aggregate/**` para a visão agregada da baseline;
- `build/reports/licenses/**` para dependências e ferramentas resolvidas;
- `build/reports/jars/**` para a inspeção dos artifacts;
- `**/build/reports/api/**` para assinatura atual e fronteiras públicas.

`if-no-files-found: warn` preserva a falha original quando uma quebra ocorre
antes da geração dos relatórios, sem mascará-la com uma segunda falha de upload.

## Cache seguro e evidência de hit/miss

O cache Gradle usa o provider `basic`, valida o Wrapper e separa chaves por
sistema e conteúdo do build. Pull requests e merge queues têm acesso
somente-leitura; apenas pushes em `main` podem gravar. Essa política impede que
código não integrado substitua o cache compartilhado da branch protegida.

No check Ubuntu, `clean test` é repetido após a execução inicial. A primeira
execução popula o build cache local e a repetição registra tasks `FROM-CACHE`,
fornecendo uma prova reproduzível de miss/hit no mesmo ambiente. O resumo do
`setup-gradle` registra separadamente se o cache remoto foi restaurado ou não.

## Proteção de `main`

O mantenedor deve configurar a regra/ruleset de `main` com:

- pull request obrigatório antes do merge;
- os três checks acima como required status checks, usando GitHub Actions como
  origem esperada;
- branch atualizada antes do merge, se essa política estiver habilitada no
  repositório;
- nenhuma permissão de bypass para automações comuns.

Os nomes são únicos e o workflow não usa filtro de paths. Depois de configurada,
qualquer falha em Ubuntu, Windows ou macOS mantém o respectivo required check
vermelho e impede o merge. `merge_group` garante que os mesmos checks sejam
produzidos para uma merge queue.

## Injeção controlada de falha

Para validar o bloqueio sem alterar produção:

1. em uma branch de teste, adicione temporariamente a um único item da matriz
   uma invocação do Wrapper para um task Gradle inexistente;
2. envie o commit e preserve o link da execução: o sistema escolhido deve
   falhar, os outros dois devem terminar porque `fail-fast` é `false`;
3. reverta integralmente o commit de injeção;
4. confirme os três checks verdes no commit final e registre ambos os links em
   `docs/validation/issue-12.md`.

Não faça merge do commit de injeção. Uma falha real recebe o mesmo tratamento:
diagnosticar pelo log e artifact do sistema afetado, corrigir na branch e exigir
uma nova execução verde dos três checks.

## Rollback

Reverter `build.yml`, este documento e o task
`java25CompatibilityTest` remove a CI e o smoke adicional sem alterar código de
produção. Em uma emergência de infraestrutura, não remova silenciosamente um
required check: registre a indisponibilidade, obtenha aprovação do
`technical_coordinator` e ajuste a proteção e este contrato na mesma mudança.
