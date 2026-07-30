# Spike desktop libGDX/LWJGL3 — Issue #14

- Estado: concluído; backend aceito pela ADR-002/D-011
- Issue: [#14](https://github.com/Balehlah/Engine_lite/issues/14)
- Dependência concluída:
  [#60](https://github.com/Balehlah/Engine_lite/issues/60)
- Branch-base: `codex/issue-14-libgdx-lwjgl3-spike`
- Branch empilhada: `codex/issue-60-windows-linux-mesa`
- Baseline: Java 21 LTS
- Compatibilidade adicional: Java 25 LTS
- libGDX: 1.14.2
- LWJGL: 3.4.1
- Plataformas suportadas: Windows e Linux

## Objetivo e limites

O spike mediu se libGDX/LWJGL3 atende integralmente o gate da
[ADR-002](../adr/ADR-002-libgdx-lwjgl3-backend.md), na matriz substituída pela
[ADR-006](../adr/ADR-006-windows-linux-desktop-support.md). A implementação
permanece isolada e removível. Ela não migra subsistemas legados, não remove
Java2D e não adiciona dependências gráficas a `engine:core`.

O pacote de prova deve:

- criar, redimensionar e encerrar uma janela LWJGL3;
- renderizar framebuffer virtual configurável com referência 320×180, escala
  inteira, nearest-neighbor e barras centralizadas;
- carregar recursos internos sem depender do diretório de trabalho;
- exercitar input, áudio e Tiled;
- liberar exatamente uma vez todos os `Disposable` possuídos;
- gerar evidências reproduzíveis e autoencerrar;
- iniciar o mesmo ZIP canônico extraído em Windows e Linux com Java 21/25.

Editor, mobile, web, migração horizontal e remoção do backend Java2D permanecem
fora do escopo.

## Matriz do gate

O backend foi aceito depois que todos os itens ficaram `PASS`, houve validação
independente de `qa_validator` e @Balehlah registrou a aprovação final.
Aprovação parcial não foi utilizada.

| Área | Esperado | Windows | Linux | Evidência |
|---|---|---:|---:|---|
| Build | `clean test` e distribuição com exit code 0 | PASS | PASS | CI da branch |
| Lifecycle | abre, redimensiona e fecha sem exceção | PASS | PASS | `lifecycle.log` |
| Viewport | fixtures 640×360, 800×600 e 1280×720 | PASS | PASS | testes e PNGs |
| Filtragem | nearest e nenhuma escala fracionária | PASS | PASS | teste e golden |
| Input/sprite | evento mínimo altera sprite renderizado | PASS | PASS | log e PNG |
| Assets | recurso interno carrega com CWD externo | PASS | PASS | `probe.log` |
| Integrações | OpenAL Soft real com driver `null`; TMX carrega | PASS | PASS | `probe.log` |
| Recursos | cada `Disposable` tem contagem final 1 | PASS | PASS | `dispose.log` |
| Packaging | o mesmo ZIP inicia/encerra em Java 21/25 | PASS | PASS | artifacts, hashes e smoke |
| Qualidade | zero defeitos bloqueadores | PASS | PASS | checklist QA |

## Fixtures de viewport

| Backbuffer | Escala | Área apresentada | Barras |
|---|---:|---:|---|
| 640×360 | 2× | 640×360 | nenhuma |
| 800×600 | 2× | 640×360 | 80 px laterais e 120 px superior/inferior |
| 1280×720 | 4× | 1280×720 | nenhuma |

As dimensões são medidas em pixels do backbuffer. A captura é do framebuffer
completo e a matemática também possui teste determinístico.

## Distribuição e natives

O ZIP não recebe payload da plataforma não suportada nem tooling Mesa. O build:

1. verifica o SHA-256 do
   `gdx-platform-1.14.2-natives-desktop.jar` upstream;
2. confirma o inventário de entrada exato;
3. cria `gdx-platform-1.14.2-natives-windows-linux.jar` com bytes nativos
   allowlisted, licença e proveniência;
4. resolve para cada módulo LWJGL somente os classifiers Windows/Linux
   contratados;
5. inspeciona nomes, launchers e conteúdo interno de todos os JARs;
6. falha se arquivo proibido ou DLL de tooling reaparecer.

## Evidências e reprodução

Comandos canônicos:

```text
gradlew.bat --no-daemon clean test -PtestRandomSeed=1414
gradlew.bat --no-daemon buildSpikeDistribution verifyDistribution recordSpikeDistributionHash
gradlew.bat --no-daemon smokeSpikeDistribution -PspikeSmokeVariant=local-java21
gradlew.bat --no-daemon verifySpikeDistributionHash
```

No Linux, use `./gradlew`. O smoke:

1. usa o ZIP previamente hashado;
2. extrai em diretório temporário;
3. inicia o launcher a partir de CWD temporário externo;
4. força o driver `null` do OpenAL Soft;
5. exige evento real recebido pelo `InputProcessor`;
6. compara cada pixel das três capturas;
7. exige cada recurso possuído descartado exatamente uma vez;
8. registra renderer, ambiente, SHA-256 do ZIP e evidências;
9. falha se o ZIP mudar durante ou depois das execuções.

No runner Windows, Mesa llvmpipe 26.1.1 é provisionado nos JDKs efêmeros 21/25
com URL e hashes fixos. `probe.log` deve conter `gl.renderer` com `llvmpipe` e
`runner.properties` registra origem, commit, licença e digests. Mesa não integra
o pacote nem os artifacts.

## Baseline e decisão final

O baseline local Windows anterior à Issue #60 passou em Java 21 e manteve o ZIP
SHA-256
`113ed4e89538ed2c41361e1f2fe6c5af8a40fcbbc10ca2238bac4f6d44f6d562`
antes/depois do smoke. Esse valor identifica a entrada da mudança, não o artifact
final.

A execução remota anterior comprovou Ubuntu e expôs a ausência de WGL no runner
Windows. A PR #61 incorporou Mesa auditável e a matriz pós-integração da PR #59
[30559042291](https://github.com/Balehlah/Engine_lite/actions/runs/30559042291)
passou nos dois sistemas e JDKs. Os links, hashes e artifacts estão registrados em
[`docs/validation/issue-14.md`](../validation/issue-14.md) e
[`docs/validation/issue-60.md`](../validation/issue-60.md).

O `qa_validator` registrou **PASS, zero defeitos bloqueadores** e @Balehlah
aprovou a regra **Aceitar** em 2026-07-30. A ADR-002/D-011 seleciona
libGDX/LWJGL3 e preserva Java2D como fallback legado; migração horizontal
permanece fora desta entrega.

## Riscos e fallback

- criação de contexto e carregamento de natives podem divergir por plataforma;
- OpenAL `null` prova backend/decodificação/alocação, não saída audível;
- Mesa é risco de supply chain mitigado por URL, commit, allowlist e hashes;
- uma curadoria incorreta é bloqueada por inventário positivo/negativo do ZIP.

Se qualquer gate obrigatório permanecer falhando, o resultado é rejeição:
remove-se somente o spike, Java2D permanece e uma nova ADR deve avaliar o
próximo candidato.
