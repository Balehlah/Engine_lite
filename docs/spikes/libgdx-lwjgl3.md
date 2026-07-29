# Spike desktop libGDX/LWJGL3 — Issue #14

- Estado: em execução; decisão do backend pendente
- Issue: [#14](https://github.com/Balehlah/Engine_lite/issues/14)
- Branch: `codex/issue-14-libgdx-lwjgl3-spike`
- Base: `9de87d9`
- Baseline: Java 21 LTS
- Compatibilidade adicional: Java 25 LTS
- libGDX: 1.14.2
- LWJGL: 3.4.1

## Objetivo e limites

Este spike mede se libGDX/LWJGL3 atende integralmente o gate da
[ADR-002](../adr/ADR-002-libgdx-lwjgl3-backend.md). A implementação permanece
isolada em uma aplicação removível. Ela não migra subsistemas legados, não
remove Java2D e não adiciona dependências gráficas a `engine:core`.

O pacote de prova deve:

- criar, redimensionar e encerrar uma janela LWJGL3;
- renderizar um sprite em framebuffer virtual configurável, com referência
  320×180, escala inteira, nearest-neighbor e barras centralizadas;
- carregar recursos internos sem depender do diretório de trabalho;
- exercitar input, áudio e carregamento Tiled;
- liberar exatamente uma vez todos os `Disposable` cuja posse pertence ao
  spike;
- gerar evidências reproduzíveis e autoencerrar;
- iniciar o ZIP canônico extraído em Windows, Linux e macOS.

Editor, mobile, web, migração horizontal e remoção do backend Java2D permanecem
fora do escopo.

## Matriz do gate

O backend só pode ser aceito quando todos os itens estiverem `PASS`, houver
validação independente de `qa_validator` e @Balehlah registrar a aprovação
final. Aprovação parcial não é permitida.

| Área | Esperado | Windows | Linux | macOS | Evidência |
|---|---|---:|---:|---:|---|
| Build | `clean test` e distribuição com exit code 0 | Pendente | Pendente | Pendente | CI da branch |
| Lifecycle | abre, redimensiona e fecha sem exceção | Pendente | Pendente | Pendente | `lifecycle.log` |
| Viewport | fixtures 640×360, 800×600 e 1280×720 | Pendente | Pendente | Pendente | testes e PNGs |
| Filtragem | nearest e nenhuma escala fracionária | Pendente | Pendente | Pendente | teste e golden |
| Input/sprite | evento mínimo altera sprite renderizado | Pendente | Pendente | Pendente | log e PNG |
| Assets | recurso interno carrega com CWD externo | Pendente | Pendente | Pendente | `probe.log` |
| Integrações | OpenAL Soft real, não `MockAudio`, com driver `null`; TMX carrega | Pendente | Pendente | Pendente | `probe.log` |
| Recursos | cada `Disposable` tem contagem final 1 | Pendente | Pendente | Pendente | `dispose.log` |
| Packaging | pacote instalado inicia e autoencerra | Pendente | Pendente | Pendente | artifacts e smoke |
| Qualidade | zero defeitos bloqueadores | Pendente | Pendente | Pendente | checklist QA |

## Fixtures de viewport

| Backbuffer | Escala | Área apresentada | Barras |
|---|---:|---:|---|
| 640×360 | 2× | 640×360 | nenhuma |
| 800×600 | 2× | 640×360 | 80 px laterais e 120 px superior/inferior |
| 1280×720 | 4× | 1280×720 | nenhuma |

As dimensões são medidas em pixels do backbuffer, inclusive em HiDPI. A captura
é do framebuffer completo; a matemática é validada separadamente em teste
determinístico para não transformar variação de driver em contrato.

## Evidências e reprodução

As evidências pequenas e duráveis ficam em
[`docs/validation/issue-14`](../validation/issue-14.md). Pacotes e relatórios
completos serão publicados por sistema operacional na CI e receberão hashes
SHA-256 no registro de validação.

Comandos canônicos:

```text
gradlew.bat --no-daemon clean test -PtestRandomSeed=1414
gradlew.bat --no-daemon buildSpikeDistribution verifyDistribution
gradlew.bat --no-daemon smokeSpikeDistribution -PspikeSmokeVariant=local-java21
```

No Linux/macOS, use `./gradlew` com os mesmos argumentos. O smoke:

1. extrai o `distZip` exato em diretório temporário;
2. inicia o script da distribuição a partir de outro diretório temporário;
3. força o driver `null` do OpenAL Soft em runners sem áudio;
4. exige o evento de cursor recebido pelo `InputProcessor` do backend;
5. compara cada pixel das três capturas com o golden matemático;
6. exige nove recursos possuídos, cada um descartado uma vez;
7. registra logs, propriedades do runner e SHA-256 do ZIP e das evidências.

No Windows/Java 21, a execução local de 2026-07-29 passou com Intel UHD
Graphics/OpenGL 4.6 e produziu o ZIP SHA-256
`101ba840cebe7e359457d88ab473a8f2544fa639d05644d193149ee702f92887`.
Uma segunda build isolada produziu o mesmo hash. Os links da matriz CI serão
registrados quando a branch for publicada. Até lá, nenhuma decisão de aceite ou
rejeição foi tomada.

## Riscos e fallback

- Natives e criação de contexto podem divergir entre as três famílias de SO.
- O smoke usa um backend OpenAL real e deve rejeitar a substituição silenciosa
  por `MockAudio`; em runners sem dispositivo físico, OpenAL Soft usa seu driver
  `null`.
- O evento de input automatizado prova o `InputProcessor` instalado; o modo
  interativo preserva a verificação manual de hardware.
- macOS/HiDPI é medido em pixels do backbuffer.
- O driver OpenAL `null` prova inicialização, decodificação e alocação de source,
  mas não substitui uma verificação audível em hardware.

Se qualquer gate obrigatório permanecer falhando, o resultado é rejeição:
remove-se somente este spike, Java2D permanece disponível e uma nova ADR deve
avaliar o próximo candidato.
