# ADR-006 — Suporte desktop restrito a Windows e Linux

- Estado: aceita
- Data: 2026-07-29
- Responsável técnico: `devops-release`
- Responsável e aprovador: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Validador obrigatório: `qa_validator`
- Issue: [#60](https://github.com/Balehlah/Engine_lite/issues/60)
- Decisão mestre: D-010
- Substitui: D-002 e somente o item 2 da ADR-001

## Contexto

A ADR-001/D-002 definiu originalmente três famílias desktop para a linha 1.0.0.
O spike da Issue #14 construiu e empacotou o candidato libGDX/LWJGL3, mas a
execução remota
[30476609058](https://github.com/Balehlah/Engine_lite/actions/runs/30476609058)
demonstrou que o runner Windows hospedado não oferece WGL utilizável e que a
plataforma removida não dispõe de ambiente acessível para uma validação gráfica
real.

O mantenedor decidiu não criar ou manter um fork do GLFW e não sustentar uma
promessa de compatibilidade sem ambiente de validação. O Windows pode receber
um rasterizador de software auditável e efêmero na CI; não existe solução
equivalente aprovada para a terceira família original.

## Decisão

1. Windows e Linux são as únicas famílias desktop suportadas pela linha 1.0.0.
2. macOS é explicitamente não suportado: não recebe artifact, native, smoke,
   required check ou promessa de compatibilidade.
3. O gate da ADR-002 mede o backend somente em Windows e Linux e continua
   experimental até a conclusão da Issue #14, a revisão de `qa_validator` e a
   aprovação final de @Balehlah.
4. O smoke Windows usa Mesa llvmpipe 26.1.1 apenas como tooling efêmero da CI.
   Origem, commit e SHA-256 do arquivo e dos dois DLLs extraídos são fixos.
5. Mesa não integra o classpath, o ZIP, artifacts publicados ou o repositório.
6. D-002, D-009, ADR-001 e as evidências fechadas das Issues #9/#12 permanecem
   inalteradas como registro histórico. Esta ADR substitui o contrato vigente
   sem reescrever a decisão original.

## Justificativa

Uma plataforma suportada exige build, pacote, smoke gráfico reproduzível,
evidência e capacidade real de diagnosticar regressões. Windows e Linux atendem
esse contrato. Reduzir a matriz explicitamente é preferível a manter um gate
permanentemente vermelho ou aceitar um falso positivo sem janela/contexto
gráfico.

Mesa llvmpipe resolve somente a limitação do runner Windows. Os DLLs são
baixados por URL HTTPS fixa, verificados antes do uso, extraídos por allowlist e
copiados apenas para os JDKs 21/25 localizados em `RUNNER_TOOL_CACHE`. O renderer
observado deve conter `llvmpipe`.

## Consequências

- a CI possui apenas `Build and test (Ubuntu)` e
  `Build and test (Windows)`;
- o ZIP contém somente natives de Windows/Linux e um JAR libGDX curado,
  reproduzível e rastreável;
- nenhum launcher recebe flag exclusiva da plataforma não suportada;
- a proteção de `main` exige somente os dois checks suportados;
- a ADR-002 preserva seu fallback e não aceita libGDX/LWJGL3 parcialmente;
- reintroduzir uma terceira família é uma decisão de arquitetura, não uma
  simples restauração de classifiers.

## Gatilhos de revisão

- ambiente de validação acessível e reproduzível para uma nova família;
- solicitação formal de suporte adicional para a linha 1.x ou para nova major;
- perda de suporte Windows/Linux pelo JDK ou backend selecionado;
- impossibilidade de reproduzir WGL com a origem e os hashes fixados;
- mudança material de licença, origem ou segurança do tooling Mesa.

## Rollback

Reverter integralmente a PR da Issue #60 restaura o código e a matriz anteriores
sem remover Java2D nem apagar evidências históricas. Esse rollback não torna a
plataforma removida novamente suportada: restaurar suporte exige nova ADR,
ambiente de validação e matriz completa aprovada por `qa_validator` e
`technical_coordinator`.
