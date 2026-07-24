# Política de versionamento e compatibilidade

Esta política deriva da
[ADR-003](adr/ADR-003-versioning-and-public-api.md) e será automatizada pela
[Issue #13](https://github.com/Balehlah/Engine_lite/issues/13).

## Linha de versões

- Releases públicas seguem Semantic Versioning.
- Tags usam `vMAJOR.MINOR.PATCH`.
- A futura `v1.0.0` é a primeira release pública com compatibilidade suportada.
- `prototype-1.0.0` e `prototype-2.0.0`, datados de 2025 no changelog, são
  marcos históricos sem tags ou baseline de API. Eles não antecedem
  semanticamente a futura `v1.0.0`.
- O primeiro RC congela a baseline da API estável; antes dele, nenhuma API do
  protótipo é prometida como estável.

## Classificação normativa de pacotes

| Categoria | Namespace | Compromisso |
|---|---|---|
| Estável | `engine.api.*` | Compatibilidade de fonte e binária durante a linha 1.x; quebra exige major |
| Interno | `engine.internal.*` | Sem garantia; não pode aparecer em assinaturas estáveis nem vazar transitivamente |
| Incubador | `engine.incubator.*` | Experimental; pode mudar em minor com changelog e guia de migração |
| Protótipo legado | Pacotes atuais `engine.core`, `engine.display`, `engine.graphics`, `engine.input`, `engine.assets`, `engine.audio`, `engine.tilemap`, `engine.physics`, `engine.io`, `engine.math` e `engine.util` | Nenhuma estabilidade até classificação explícita pela Issue #13 |
| Demo | `game.test` | Fora da API do motor |

Tipos Java `public` fora de `engine.api.*` não são automaticamente parte da API
estável.

## Compatibilidade e depreciação

- Adições compatíveis entram em versões minor.
- Correções compatíveis entram em versões patch.
- Remoções ou mudanças incompatíveis de API estável exigem versão major.
- Uma API estável deve permanecer depreciada por pelo menos uma versão minor
  antes de remoção, salvo correção de segurança documentada.
- Promoção de incubador para estável exige documentação, testes e inclusão na
  baseline.
- Código interno não pode fazer parte de parâmetros, retornos, heranças,
  exceções declaradas ou dependências transitivas da API estável.

## Changelog e tags

Cada release pública deve:

1. mover mudanças de `Unreleased` para a versão;
2. registrar data, compatibilidade, depreciações e migrações;
3. passar a checagem de baseline da API;
4. produzir tag anotada no formato definido;
5. apontar para artifacts e relatório de licenças.

## Responsabilidade e revisão

O `technical-coordinator` aprova mudanças de contrato. O `qa_validator` valida
baseline, jars e falhas controladas. Esta política deve ser revista no primeiro
RC e em toda proposta de versão major.
