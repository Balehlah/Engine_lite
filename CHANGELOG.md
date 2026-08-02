# Changelog

Todas as mudancas notaveis neste projeto serao documentadas neste arquivo.

O formato e baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## Política da futura 1.0.0

Os registros `prototype-1.0.0` e `prototype-2.0.0` abaixo são marcos históricos
do protótipo em 2025. Eles não tiveram tags, artifacts publicados ou baseline de
API e, portanto, não constituem releases SemVer.

A futura `v1.0.0` será a primeira release pública suportada do Engine Lite. A
política completa está em [docs/versioning.md](docs/versioning.md).

## [Unreleased]

### Adicionado

- Licença Apache-2.0, notices de terceiros e inventário obrigatório de assets.
- Versão central `1.0.0-SNAPSHOT` e `engine.api.EngineVersion`.
- Baseline de assinatura para `engine.api.*`, gate de vazamentos internos,
  relatório de licenças e inspeção automatizada dos JARs.
- Spike desktop removível com libGDX 1.14.2 e LWJGL 3.4.1, framebuffer virtual
  320×180, escala inteira, nearest-neighbor e barras centralizadas.
- Distribuição ZIP reproduzível, launcher multiplataforma e smoke
  autoencerrável com evidências de lifecycle, input, assets, áudio, Tiled,
  viewport e descarte.
- ADR-006/D-010, que torna Windows e Linux as únicas famílias desktop
  suportadas pela linha 1.0.0.
- ADR-002/D-011, que aceita libGDX/LWJGL3 como backend desktop e preserva
  Java2D como fallback legado.
- Provisionamento efêmero e auditável de Mesa llvmpipe 26.1.1 para o smoke
  Windows hospedado.
- Scheduler incubador backend-neutral com clock real/fake, acumulador puro,
  fixed timestep configurável, clamp, catch-up limitado, `alpha`, pause,
  single-step, time scale e telemetria explícita.
- Adapter de loop e overlay de métricas para o backend libGDX, com render
  independente da quantidade de updates lógicos.
- Input incubador backend-neutral com fila ordenada e limitada, snapshots
  imutáveis por tick, bordas `pressed`/`released`, mouse virtual/delta/scroll,
  foco e `FakeInput` determinístico.
- Adapter libGDX que reduz callbacks a eventos, aplica `screenToVirtual` por
  tick após resize/DPI e identifica explicitamente barras do viewport.
- `GameContext` incubador por execução, lifecycle determinístico de cenas,
  ownership único de entidades/eventos/assets/recursos, autoridade de shutdown
  exclusiva do host e métricas de leak.
- Adapter libGDX que conecta o novo lifecycle ao scheduler fixed timestep sem
  ampliar a API estável ou contaminar `engine:core` com o backend.

### Alterado

- Todos os JARs próprios do Engine Lite recebem versão e notices consistentes
  em `META-INF`.
- `clean test` também valida distribuição, API, dependências e assets.
- A CI desktop passa a construir e executar o mesmo ZIP do spike em Java 21 e
  25 nos runners Windows e Linux.
- JLayer, JOrbis e os componentes nativos de LWJGL são inventariados com
  licenças e proveniência no pacote.
- A distribuição remove classifiers e arquivos nativos da plataforma não
  suportada; o JAR `gdx-platform` é curado por allowlist reproduzível.
- O smoke libGDX registra a política e as métricas do scheduler em `timing.log`
  sem alterar os goldens de viewport.
- O smoke libGDX registra snapshots em `input.log`, exige fila sem overflow e
  prova eventos físicos distintos em barras vazias e dentro do viewport.

### Documentação

- Registradas as decisões de produto, plataformas, Java, licença, API,
  viewport e backend da Issue #9.
- Adicionados ADRs, auditoria de fundação, roadmap e política de versionamento.
- Concluído o gate mensurável do spike libGDX/LWJGL3 com decisão aceita,
  fallback preservado e QA independente sem bloqueadores.
- Documentado o contrato estável, o uso de `api`/`implementation`, a atualização
  aprovada da baseline e o processo de atribuição.
- Adicionados especificação reproduzível e registro de evidências da Issue #14.
- Documentada a substituição histórica de D-002 por D-010 e a validação da
  Issue #60.
- Adicionado o registro reproduzível de implementação e validação da Issue #15.
- Adicionado o contrato e o registro reproduzível de validação da Issue #16.
- Adicionado o contrato e o registro reproduzível de validação da Issue #17.

---

## [prototype-2.0.0] - 2025-11-25

### Adicionado

#### Sistema de Audio (`engine.audio`)
- **`Sound.java`** - Efeitos sonoros curtos carregados na memoria
  - Playback instantaneo
  - Controle de volume (0.0 - 1.0)
  - Controle de pan (stereo: -1.0 esquerda, 1.0 direita)
  - Suporte a loop
  - Controle de posicao e duracao
- **`Music.java`** - Musica de fundo com streaming
  - Streaming para arquivos grandes (nao carrega na memoria)
  - Playback em thread separada
  - Pause/resume
  - Loop configuravel
- **`AudioManager.java`** - Gerenciador central de audio
  - Cache de sons
  - Controle de musica de fundo
  - Volumes separados (master, sound, music)
  - Sistema de mute
  - **Audio Espacial**: Sons posicionados no mundo com:
    - Volume baseado em distancia (linear falloff)
    - Pan automatico baseado na posicao horizontal
    - Posicao do listener configuravel

#### Sistema de Tilemap (`engine.tilemap`)
- **`Tileset.java`** - Conjunto de tiles
  - Carrega spritesheet e divide em tiles
  - Cache de sprites por tile
  - Propriedades de colisao por tile
- **`Tilemap.java`** - Mapa de tiles 2D
  - Multiplas camadas
  - Sistema de colisao integrado
  - Colisao automatica baseada no tileset
  - Conversao mundo <-> tile
  - Deteccao de colisao com retangulos
- **`TilemapRenderer.java`** - Renderizador otimizado
  - Culling automatico (so renderiza tiles visiveis)
  - Suporte a camera
  - Renderizacao separada de background/foreground
  - Debug: grid e visualizacao de colisao

#### Sistema de Fisica/Colisao (`engine.physics`)
- **`AABB.java`** - Axis-Aligned Bounding Box
  - Deteccao de colisao AABB vs AABB
  - Deteccao de contencao (contains)
  - Calculo de intersecao
  - Calculo de overlap/penetracao
  - MTV (Minimum Translation Vector) para resolucao
  - Expansao para movimento (broad-phase)
- **`Raycast.java`** - Sistema de raycasting
  - Raycast vs AABB
  - Raycast vs Circulo
  - Raycast vs Linha
  - Informacoes de hit: ponto, normal, distancia
  - Factory methods para facilitar criacao
- **`Collision.java`** - Utilitarios de colisao
  - Ponto vs Retangulo
  - Ponto vs Circulo
  - Retangulo vs Retangulo
  - Circulo vs Circulo
  - Circulo vs Retangulo
  - Linha vs Linha (com ponto de intersecao)
  - Linha vs Retangulo
  - Calculo de vetores de separacao
  - Sweep test (AABB em movimento)
  - Broad-phase simplificado

#### Sistema de Particulas (`engine.graphics`)
- **`Particle.java`** - Particula individual
  - Posicao, velocidade, aceleracao
  - Rotacao e escala
  - Cor RGBA com fade
  - Sistema de vida
- **`ParticleEmitter.java`** - Configuracao de emissao
  - Area de spawn (retangulo ou circulo)
  - Velocidade e angulo configuravel
  - Gravidade
  - Escala e rotacao
  - Cor inicial/final com interpolacao
  - Emissao continua ou burst
  - **Presets prontos:**
    - `createFire()` - Fogo
    - `createSmoke()` - Fumaca
    - `createExplosion()` - Explosao
    - `createSparks()` - Faiscas
    - `createSnow()` - Neve
- **`ParticleSystem.java`** - Gerenciador de particulas
  - Pool de particulas (reutilizacao, sem garbage)
  - Multiplos emissores
  - Renderizacao com sprites ou quadrados
  - Suporte a camera

#### Sistema de Serializacao (`engine.io`)
- **`SceneSerializer.java`** - Salvar/carregar cenas
  - Formato texto legivel
  - Salvamento de entidades (nome, tag, transform)
  - Propriedades customizadas extensiveis
  - Salvamento de tilemaps
  - Carregamento com dados estruturados

### Alterado

- **`build.bat`** - Atualizado para incluir novos modulos:
  - `engine.audio`
  - `engine.tilemap`
  - `engine.physics`
  - `engine.io`

### Estrutura de Arquivos

```
src/engine/
├── audio/           [NOVO]
│   ├── AudioManager.java
│   ├── Music.java
│   └── Sound.java
├── tilemap/         [NOVO]
│   ├── Tilemap.java
│   ├── TilemapRenderer.java
│   └── Tileset.java
├── physics/         [NOVO]
│   ├── AABB.java
│   ├── Collision.java
│   └── Raycast.java
├── io/              [NOVO]
│   └── SceneSerializer.java
├── graphics/
│   ├── Particle.java        [NOVO]
│   ├── ParticleEmitter.java [NOVO]
│   ├── ParticleSystem.java  [NOVO]
│   └── ... (existentes)
└── ... (existentes)
```

---

## [prototype-1.0.0] - 2025-11-25

### Adicionado

#### Core (`engine.core`)
- **`Game.java`** - Game loop profissional
  - Fixed timestep para update (60 UPS padrao)
  - Render livre ou com limite de FPS
  - Metricas de FPS/UPS
- **`Timer.java`** - DeltaTime de alta precisao
  - Time scale (slow-motion, pause)
  - Limite de deltaTime (evita spiral of death)
- **`Scene.java`** - Base para cenas
- **`SceneManager.java`** - Gerenciador de cenas
- **`Entity.java`** - Base para entidades
- **`Component.java`** - Base para ECS

#### Display (`engine.display`)
- **`Window.java`** - Janela com BufferStrategy

#### Graphics (`engine.graphics`)
- **`Renderer.java`** - Renderer principal
- **`Sprite.java`** - Sprites com transformacoes
- **`Animation.java`** - Animacoes
- **`Camera.java`** - Camera 2D
- **`ColorPalette.java`** - Paletas de cores
- **`DrawUtils.java`** - Primitivas

#### Input (`engine.input`)
- **`Input.java`** - Facade unificada
- **`Keyboard.java`** - Teclado
- **`Mouse.java`** - Mouse

#### Assets (`engine.assets`)
- **`AssetManager.java`** - Cache de assets
- **`TextureLoader.java`** - Carregamento de texturas

#### Math (`engine.math`)
- **`Vector2.java`** - Vetor 2D imutavel
- **`Rectangle.java`** - Retangulo

#### Util (`engine.util`)
- **`Logger.java`** - Sistema de log
- **`RandomUtils.java`** - Utilitarios de aleatoriedade

---

## Proximas Versoes (Planejado)

- [ ] Sistema de audio avancado (efeitos, reverb)
- [ ] Editor de tilemaps
- [ ] Sistema de navegacao (pathfinding)
- [ ] Suporte a gamepad
- [ ] Sistema de UI
