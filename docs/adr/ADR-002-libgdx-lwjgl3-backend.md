# ADR-002 — Gate do backend libGDX/LWJGL3

- Estado: aceita como experimento; decisão do backend pendente
- Data: 2026-07-24
- Responsável e aprovador final: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Validador obrigatório: `qa_validator`
- Issue da decisão: [#9](https://github.com/Balehlah/Engine_lite/issues/9)
- Spike executor: [#14](https://github.com/Balehlah/Engine_lite/issues/14)

## Contexto

Java2D mantém o protótipo executável, mas não prova os requisitos de GPU,
shaders, batching, natives, lifecycle e packaging da futura 1.0.0.
libGDX/LWJGL3 é candidato, não decisão final. A migração antes de um spike
reproduzível criaria congelamento precoce e acoplamento de alto custo.

## Decisão

Autorizar um spike isolado de libGDX/LWJGL3. O backend será aceito somente quando
todos os itens obrigatórios abaixo passarem e suas evidências estiverem
vinculadas à Issue #14.

## Matriz mensurável do gate

| Área | Critério de aprovação | Evidência obrigatória |
|---|---|---|
| Build | `clean test` e distribuição terminam com exit code 0 em Windows, Linux e macOS | Logs e artifacts da CI por SO |
| Lifecycle | Janela abre, redimensiona e fecha sem exceção não tratada | Smoke autoencerrável e logs |
| Viewport | 320×180 produz 2× em 640×360, 2× em 800×600 com barras 80/120 e 4× em 1280×720 | Testes e screenshots dos três tamanhos |
| Filtragem | Apresentação usa nearest-neighbor; nenhuma escala fracionária é aceita | Teste de configuração e golden |
| Input e sprite | Um sprite e input mínimo funcionam no smoke | Log de eventos e screenshot |
| Assets | Asset carrega com o processo iniciado fora da raiz do repositório | Execução com CWD alternativo |
| Integrações | Probes de áudio e Tiled terminam com sucesso ou documentam rejeição impeditiva | Logs dos probes |
| Recursos | Cada `Disposable` criado pelo spike é liberado exatamente uma vez | Contadores e log de dispose |
| Packaging | O pacote gerado inicia e encerra nos três SOs | Artifacts e smoke por SO |
| Qualidade | Zero defeitos bloqueadores abertos no domínio do spike | Checklist do `qa_validator` |

## Regra de decisão

- **Aceitar:** todos os itens obrigatórios passam, o `qa_validator` confirma a
  matriz e @Balehlah registra a aprovação final.
- **Rejeitar:** qualquer item obrigatório continua falhando ao encerrar a
  Issue #14, ou uma limitação estrutural impede cumprir o contrato 1.0.0.
- Aprovação parcial não aceita o backend.
- Até uma dessas decisões ser registrada, o estado permanece experimental.

## Fallback

Enquanto o spike estiver pendente, Java2D permanece isolado como backend legado
da demo. Em caso de rejeição:

1. remover somente o spike, sem apagar o protótipo;
2. manter `engine:core` independente de AWT e libGDX;
3. bloquear migrações horizontais e tarefas dependentes de GPU;
4. abrir nova ADR para o próximo candidato;
5. continuar apenas trabalho que não dependa da escolha de backend.

## Justificativa

O gate converte preferência tecnológica em evidência reproduzível e impede que
natives ou APIs do candidato contaminem o core antes de validação.

## Consequências

- A ADR-002 não declara libGDX/LWJGL3 aceito nesta data.
- Issues dependentes do backend devem aguardar a conclusão da #14.
- A decisão final preservará esta matriz e apontará para os artifacts usados.

## Gatilhos de revisão

- conclusão ou inviabilidade formal da Issue #14;
- alteração material de suporte de libGDX/LWJGL3;
- mudança nos requisitos de plataforma ou packaging.

## Rollback

O spike deve permanecer removível como uma unidade. A rejeição não autoriza
remover o backend Java2D legado nem escolher outro backend sem nova ADR.
