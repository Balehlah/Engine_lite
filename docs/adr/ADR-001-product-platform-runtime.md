# ADR-001 — Produto, plataformas e runtime Java

- Estado: aceita
- Data: 2026-07-24
- Responsável e aprovador: [@Balehlah](https://github.com/Balehlah)
  (`technical-coordinator`)
- Issue: [#9](https://github.com/Balehlah/Engine_lite/issues/9)

## Contexto

O repositório mistura um motor em `engine.*` e uma demo em `game.test`. O README
aceitava uma faixa aberta de versões Java, e o programa 1.0.0 exige validação
desktop multiplataforma. Sem decisões explícitas, build, API e distribuição
poderiam congelar contratos incompatíveis.

## Decisão

1. O Engine Lite é um motor 2D pixel-art reutilizável. A demo é somente uma
   consumidora de referência.
2. As famílias desktop suportadas são Windows, Linux e macOS.
3. Java 21 LTS é a baseline de compilação e execução da linha 1.x.
4. Java 25 LTS recebe smoke de compatibilidade, mas não eleva o bytecode mínimo.
5. Estas decisões só podem ser alteradas por uma ADR substituta.

## Justificativa

Separar produto e demo protege o core contra regras específicas de um jogo. A
matriz dos três sistemas operacionais corresponde ao objetivo de distribuição
desktop. Uma baseline LTS única torna build e suporte reproduzíveis, enquanto a
validação em Java 25 reduz risco de atualização futura.

## Consequências

- A Issue #10 deve fixar a toolchain em Java 21.
- A Issue #12 deve testar as três famílias de SO e incluir smoke em Java 25.
- O core não pode depender de código da demo.
- Recursos exclusivos de Java posterior a 21 não entram na linha 1.x.
- Mobile e web não fazem parte do suporte 1.0.0.

## Gatilhos de revisão

- uma dependência obrigatória deixar de suportar Java 21;
- uma família desktop perder suporte pelo backend escolhido;
- o produto deixar de ser um motor reutilizável;
- preparação da próxima versão major.

## Rollback

Uma nova ADR deve identificar esta decisão como substituída, registrar a nova
baseline e avaliar build, CI, distribuição e API. O histórico desta ADR deve ser
preservado.
