# ADR-003 — Versionamento e API pública

- Estado: aceita
- Data: 2026-07-24
- Responsável e aprovador: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Issue: [#9](https://github.com/Balehlah/Engine_lite/issues/9)
- Aplicação técnica: [#13](https://github.com/Balehlah/Engine_lite/issues/13)

## Contexto

O changelog histórico usa os números 1.0.0 e 2.0.0, mas não há tags, artefatos
publicados ou baseline auditada de API. As classes públicas atuais também não
distinguem contrato estável de detalhe de implementação.

## Decisão

1. A futura 1.0.0 será a primeira release pública suportada sob SemVer.
2. Os registros de 2025 permanecem no changelog como
   `prototype-1.0.0` e `prototype-2.0.0`; eles não criam compatibilidade.
3. Tags de release usarão `vMAJOR.MINOR.PATCH`.
4. O freeze da API estável ocorrerá somente no primeiro RC.
5. Os namespaces normativos são:
   - `engine.api.*`: API estável e protegida por baseline;
   - `engine.internal.*`: implementação sem garantia de compatibilidade;
   - `engine.incubator.*`: API experimental, documentada como tal.
6. Os pacotes legados atuais em `engine.*` permanecem protótipo até serem
   classificados ou migrados pela Issue #13.

## Justificativa

O número escrito em um changelog de protótipo não substitui uma release
rastreável. Namespaces explícitos permitem congelar somente a superfície que o
motor consegue sustentar, sem estabilizar acidentalmente todas as classes
`public`.

## Consequências

- Quebras em `engine.api.*` exigem versão major após 1.0.0.
- `engine.internal.*` pode mudar sem aviso e não pode vazar pela API estável.
- `engine.incubator.*` pode mudar em minor, sempre com changelog e migração.
- A Issue #13 deve criar baseline, checagem de jars e versão central.
- Nenhum pacote atual é estável apenas por possuir tipos `public`.

## Gatilhos de revisão

- início do primeiro RC;
- detecção de vazamento transitivo de pacote interno;
- promoção ou retirada de uma API incubadora;
- necessidade de exceção incompatível por segurança.

## Rollback

Uma ADR substituta deve preservar a distinção entre histórico do protótipo e
releases públicas. Uma baseline publicada nunca pode ser apagada para ocultar
uma quebra.
