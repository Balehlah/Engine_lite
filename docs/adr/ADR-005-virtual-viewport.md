# ADR-005 — Viewport virtual de referência

- Estado: aceita provisoriamente
- Data: 2026-07-24
- Responsável e aprovador: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Issue: [#9](https://github.com/Balehlah/Engine_lite/issues/9)
- Validação técnica: [#22](https://github.com/Balehlah/Engine_lite/issues/22)

## Contexto

O protótipo permite resolução interna configurável, mas usa escala `float`.
Pixel art requer dimensões previsíveis, nearest-neighbor, escala inteira e
tratamento explícito quando o backbuffer é menor que a resolução virtual.

## Decisão

1. 320×180 é a resolução virtual de referência provisória.
2. A resolução permanece configurável; 320×180 não deve ser constante oculta.
3. A escala é `floor(min(backbuffer/virtual))` e nunca fracionária.
4. Nearest-neighbor é obrigatório.
5. A imagem é centralizada com letterbox/pillarbox.
6. Backbuffers menores usam degraded mode explícito; não silenciam downscale
   fracionário como pixel-perfect.

## Justificativa

320×180 oferece aspecto 16:9 e fixtures inteiras simples. A escolha já está
incorporada aos critérios da Issue #22 e permite validar cálculo sem depender do
backend.

## Consequências

- 640×360 resulta em 2×.
- 800×600 apresenta 640×360 em 2×, com barras laterais de 80 pixels e barras
  superior/inferior de 120 pixels.
- 1280×720 resulta em 4×.
- Conversões entre tela e virtual devem tratar áreas das barras como vazias.

## Gatilhos de revisão

- o spike de backend demonstrar limitação material;
- as fixtures ou testes de high-DPI da Issue #22 falharem;
- requisitos de produto exigirem outro aspecto de referência.

## Rollback

Uma nova ADR pode alterar a referência, mas deve preservar configuração,
nearest-neighbor, escala inteira, centralização e uma estratégia mensurável de
degraded mode.
