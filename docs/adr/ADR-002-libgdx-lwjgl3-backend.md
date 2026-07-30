# ADR-002 — Gate do backend libGDX/LWJGL3

- Estado: aceita; libGDX/LWJGL3 aprovado como backend desktop
- Data: 2026-07-24
- Data da decisão final: 2026-07-30
- Responsável e aprovador final: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Validador obrigatório: `qa_validator`
- Issue da decisão: [#9](https://github.com/Balehlah/Engine_lite/issues/9)
- Spike executor: [#14](https://github.com/Balehlah/Engine_lite/issues/14)
- Decisão mestre final: D-011

## Contexto

Na abertura deste gate, Java2D mantinha o protótipo executável, mas não provava
os requisitos de GPU, shaders, batching, natives, lifecycle e packaging da
futura 1.0.0.
libGDX/LWJGL3 era candidato, não decisão final. A migração antes de um spike
reproduzível teria criado congelamento precoce e acoplamento de alto custo.

## Decisão

Autorizar um spike isolado de libGDX/LWJGL3, condicionado aos itens obrigatórios
abaixo e às evidências vinculadas à Issue #14.

Em 2026-07-30, após a integração da PR #61, os dois checks da PR #59 passaram
em Windows/Linux e Java 21/25, os quatro smokes executaram o mesmo ZIP e o
`qa_validator` confirmou a matriz com zero defeitos bloqueadores. @Balehlah
aprovou explicitamente a decisão final: **aceitar libGDX/LWJGL3 como backend
desktop da linha 1.0.0, preservando Java2D como fallback legado**.

## Matriz mensurável do gate

| Área | Critério de aprovação | Evidência obrigatória |
|---|---|---|
| Build | `clean test` e distribuição terminam com exit code 0 em Windows e Linux | Logs e artifacts da CI por SO suportado |
| Lifecycle | Janela abre, redimensiona e fecha sem exceção não tratada | Smoke autoencerrável e logs |
| Viewport | 320×180 produz 2× em 640×360, 2× em 800×600 com barras 80/120 e 4× em 1280×720 | Testes e screenshots dos três tamanhos |
| Filtragem | Apresentação usa nearest-neighbor; nenhuma escala fracionária é aceita | Teste de configuração e golden |
| Input e sprite | Um sprite e input mínimo funcionam no smoke | Log de eventos e screenshot |
| Assets | Asset carrega com o processo iniciado fora da raiz do repositório | Execução com CWD alternativo |
| Integrações | Probes de áudio e Tiled terminam com sucesso ou documentam rejeição impeditiva | Logs dos probes |
| Recursos | Cada `Disposable` criado pelo spike é liberado exatamente uma vez | Contadores e log de dispose |
| Packaging | O pacote gerado inicia e encerra em Windows e Linux | Artifacts e smoke por SO suportado |
| Qualidade | Zero defeitos bloqueadores abertos no domínio do spike | Checklist do `qa_validator` |

## Regra de decisão

- **Aceitar:** todos os itens obrigatórios passam, o `qa_validator` confirma a
  matriz e @Balehlah registra a aprovação final.
- **Rejeitar:** qualquer item obrigatório continua falhando ao encerrar a
  Issue #14, ou uma limitação estrutural impede cumprir o contrato 1.0.0.
- Aprovação parcial não aceita o backend.
- A decisão registrada em 2026-07-30 aplicou a regra **Aceitar** integralmente.

## Fallback

Durante o spike, Java2D permaneceu isolado como backend legado da demo. A
aceitação do backend não autoriza removê-lo automaticamente: sua substituição
na demo e a migração horizontal pertencem a tarefas posteriores. Se uma futura
revisão rejeitar libGDX/LWJGL3 por nova ADR:

1. remover somente o spike, sem apagar o protótipo;
2. manter `engine:core` independente de AWT e libGDX;
3. bloquear migrações horizontais e tarefas dependentes de GPU;
4. abrir nova ADR para o próximo candidato;
5. continuar apenas trabalho que não dependa da escolha de backend.

## Evidência da decisão final

- PR empilhada integrada:
  [#61](https://github.com/Balehlah/Engine_lite/pull/61), merge `628bfb3`;
- matriz pós-integração da PR #59:
  [execução 30559042291](https://github.com/Balehlah/Engine_lite/actions/runs/30559042291);
- `Build and test (Ubuntu)` e `Build and test (Windows)` verdes;
- Java 21/25 e os quatro smokes do mesmo ZIP aprovados;
- SHA-256 canônico:
  `9f9b53677c975233ebc72ad3d4f457e670e4f05419c6f6e749eff86a193f8d15`;
- revisão independente de `qa_validator`: **PASS, zero defeitos bloqueadores**;
- aprovação final explícita de @Balehlah em 2026-07-30.

## Justificativa

O gate converte preferência tecnológica em evidência reproduzível e impede que
natives ou APIs do candidato contaminem o core antes de validação.

## Consequências

- libGDX/LWJGL3 é o backend desktop aceito para a linha 1.0.0.
- Java2D permanece como fallback legado até migração explícita.
- Issues dependentes do backend podem prosseguir após a conclusão formal da
  #14, respeitando boundaries e estabilidade de API.
- Esta matriz e os artifacts usados permanecem como gate de regressão.
- A [ADR-006](ADR-006-windows-linux-desktop-support.md) substitui a plataforma
  da D-002 e restringe esta matriz às famílias atualmente suportadas.

## Gatilhos de revisão

- conclusão ou inviabilidade formal da Issue #14;
- alteração material de suporte de libGDX/LWJGL3;
- mudança nos requisitos de plataforma ou packaging.

## Rollback

O spike deve permanecer removível como uma unidade. A rejeição não autoriza
remover o backend Java2D legado nem escolher outro backend sem nova ADR.
