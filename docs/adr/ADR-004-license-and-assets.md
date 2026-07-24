# ADR-004 — Licença do código e política de assets

- Estado: aceita
- Data: 2026-07-24
- Responsável e aprovador: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Issue: [#9](https://github.com/Balehlah/Engine_lite/issues/9)
- Aplicação técnica: [#13](https://github.com/Balehlah/Engine_lite/issues/13)

## Contexto

O repositório não contém `LICENSE`, notices ou inventário de assets. Distribuir
código e conteúdo sem origem comprovada cria risco jurídico e impede uma
release pública confiável.

## Decisão

1. O código do Engine Lite será licenciado sob Apache License 2.0.
2. Um asset só pode entrar no repositório quando possuir autor/origem, licença,
   localização e requisitos de atribuição registrados.
3. São aceitos por padrão assets próprios, CC0, CC-BY 4.0 ou outra licença
   comprovadamente compatível após revisão.
4. Assets com licença desconhecida, restrição incompatível ou ausência de
   permissão não podem ser distribuídos.
5. Dependências e conteúdo de terceiros devem constar nos notices e relatórios
   definidos pela Issue #13.

## Justificativa

Apache-2.0 permite uso amplo e inclui concessão explícita de patentes. O
inventário de assets impede que a licença do código seja confundida com a
licença de arte, áudio, fontes ou mapas.

## Consequências

- A Issue #13 deve adicionar `LICENSE`, `THIRD_PARTY_NOTICES.md`,
  `assets/ATTRIBUTION.md` e o relatório de licenças.
- O repositório continua sem release licenciada até essa aplicação.
- Assets sem evidência devem permanecer fora do produto e dos artifacts.

## Gatilhos de revisão

- identificação de licença incompatível;
- adoção de dependência ou asset com obrigações novas;
- mudança do modelo de distribuição;
- contribuição que exija política adicional.

## Rollback

Uma mudança de licença exige nova ADR e revisão jurídica das contribuições já
recebidas. O histórico de autoria e atribuição nunca deve ser removido.
