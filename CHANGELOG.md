# Changelog

Todas as mudancas notaveis neste projeto serao documentadas neste arquivo.

O formato e baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

---

## [2.0.0] - 2025-11-25

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

## [1.0.0] - 2025-11-25

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

