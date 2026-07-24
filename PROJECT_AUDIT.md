# Auditoria de fundação do Engine Lite

- Data da auditoria: 2026-07-24
- Responsável: [@Balehlah](https://github.com/Balehlah), no papel de
  `technical-coordinator`
- Escopo: estado da branch `main` antes da execução da
  [Issue #9](https://github.com/Balehlah/Engine_lite/issues/9)

## Resumo executivo

O repositório contém um protótipo funcional de motor 2D em Java/AWT, uma demo em
`game.test`, scripts manuais de compilação e binários versionados. Ele ainda não
possui uma linha de release pública, build declarativo, CI, licença aplicada,
baseline de API ou backend validado para a futura versão 1.0.0.

A Issue #9 fecha as ambiguidades de produto e governança. Ela não autoriza
alterações de build, backend ou runtime; essas mudanças continuam nas issues
dependentes.

## Evidência observada

| Área | Evidência na `main` | Impacto |
|---|---|---|
| Produto | `README.md` descreve um motor e `src/game/test` contém uma demo | O motor é o produto; a demo é consumidora de referência |
| Build | `build.bat` e `build.sh` enumeram fontes manualmente | A reprodução e os módulos serão tratados pela Issue #10 |
| Runtime Java | O README aceitava Java 11+ e o código usa recursos disponíveis desde Java 10 | A toolchain precisava de uma versão única e suportada |
| Backend | `Window` e `Renderer` dependem de AWT/Java2D | O backend de release depende do spike da Issue #14 |
| Pixel-perfect | `Renderer.end()` calcula escala `float` | A escala inteira e o degraded mode ainda precisam da Issue #22 |
| API | Classes públicas estão distribuídas diretamente em `engine.*` | Nenhum pacote atual pode ser presumido estável |
| Versões | O changelog registra 1.0.0 e 2.0.0, mas o repositório não possui tags | Esses números são marcos históricos do protótipo |
| Licença | Não há arquivo `LICENSE` nem inventário de assets/dependências | A política é decidida agora e aplicada pela Issue #13 |
| CI | Não há workflow versionado | A matriz dos três sistemas operacionais pertence à Issue #12 |
| Governança | Não existiam ADRs, roadmap ou registro de decisões | A Issue #9 cria a fonte de verdade |

## Decisões fechadas

As decisões aprovadas pelo mantenedor estão no
[registro mestre](ARCHITECTURE_DECISIONS.md):

- o Engine Lite, e não a demo, é o produto;
- Windows, Linux e macOS são as famílias desktop suportadas;
- Java 21 LTS é a baseline e Java 25 LTS é a linha de compatibilidade;
- Apache-2.0 é a licença escolhida para o código;
- a futura 1.0.0 inaugura o contrato SemVer público;
- os namespaces estável, interno e incubador têm políticas distintas;
- 320×180 é a resolução virtual de referência provisória;
- libGDX/LWJGL3 depende de gate mensurável, aprovador e fallback;
- decisões aceitas só são revertidas por nova ADR.

## Riscos abertos e encaminhamento

| Risco | Estado após a Issue #9 | Próxima ação |
|---|---|---|
| Build não reproduzível | Contrato definido, implementação pendente | Issue #10 |
| Baseline e regressões desconhecidas | Fora do escopo desta decisão | Issue #11 |
| Ausência de CI multiplataforma | Três famílias de SO aprovadas | Issue #12 |
| Licença ainda não materializada | Apache-2.0 e política de assets aprovadas | Issue #13 |
| Backend de release desconhecido | Gate e fallback definidos | Issue #14 |
| Escala fracionária no protótipo | 320×180 e escala inteira aprovados | Issue #22 |
| API pública ainda não congelada | Namespaces e regras definidos | Issue #13 |

## Conclusão da auditoria

Não há impedimento técnico para registrar as decisões de fundação. Há
impedimentos explícitos para implementar build, backend, distribuição e
baseline de API antes das respectivas issues. A ordem de execução está
documentada em [ROADMAP.md](ROADMAP.md).
