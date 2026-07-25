# 🎮 Engine Lite

Engine 2D profissional em Java puro para jogos pixel art. Arquitetura modular, limpa e extensível.

## Estado do projeto

O código atual é um protótipo pré-1.0 baseado em Java2D. O produto é o motor;
`game.test` é apenas uma demo consumidora. A futura versão 1.0.0 será a primeira
release pública com contrato SemVer e API estável auditada.

As decisões de fundação estão em
[ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md), o plano de execução em
[ROADMAP.md](ROADMAP.md) e a auditoria do protótipo em
[PROJECT_AUDIT.md](PROJECT_AUDIT.md).

## 📋 Características

- ✅ **Game Loop Profissional** - Fixed timestep para física, render livre para FPS máximo
- ✅ **DeltaTime Real** - Timer de alta precisão com time scale
- ✅ **Sistema de Cenas** - SceneManager com transições suaves
- ✅ **Entidades & Componentes** - Base para ECS
- ✅ **Input Robusto** - Teclado + Mouse com estados (pressed, released, down)
- ✅ **Câmera 2D** - Follow, zoom, shake, bounds
- ✅ **Sprites & Animações** - Spritesheets, flip, scale, rotation
- ✅ **Renderer Otimizado** - Pixel-perfect, resolução virtual
- ✅ **Asset Manager** - Cache inteligente com hot-reload
- ✅ **Math Library** - Vector2, Rectangle
- ✅ **Paletas de Cores** - PICO-8, GameBoy, NES, CGA
- ✅ **Sistema de Áudio** - Sons, música, áudio espacial
- ✅ **Tilemap** - Mapas baseados em tiles com colisão
- ✅ **Física/Colisão** - AABB, raycasting, detecção de colisão
- ✅ **Partículas** - Sistema eficiente com presets
- ✅ **Serialização** - Salvar/carregar cenas e tilemaps

## 📁 Estrutura

```
src/
├── engine/
│   ├── core/           # Núcleo da engine
│   │   ├── Game.java         # Game loop principal
│   │   ├── Timer.java        # DeltaTime e FPS
│   │   ├── Scene.java        # Base de cenas
│   │   ├── SceneManager.java # Gerenciador de cenas
│   │   ├── Entity.java       # Base de entidades
│   │   └── Component.java    # Base para ECS
│   │
│   ├── display/        # Janela
│   │   └── Window.java       # Janela + BufferStrategy
│   │
│   ├── graphics/       # Renderização
│   │   ├── Renderer.java     # Renderer principal
│   │   ├── Sprite.java       # Sprites
│   │   ├── Animation.java    # Animações
│   │   ├── Camera.java       # Câmera 2D
│   │   ├── ColorPalette.java # Paletas pixel art
│   │   ├── DrawUtils.java    # Primitivas
│   │   ├── Particle.java     # Partícula individual
│   │   ├── ParticleEmitter.java # Configuração de emissão
│   │   └── ParticleSystem.java  # Sistema de partículas
│   │
│   ├── input/          # Input
│   │   ├── Input.java        # Facade unificada
│   │   ├── Keyboard.java     # Teclado
│   │   └── Mouse.java        # Mouse
│   │
│   ├── assets/         # Assets
│   │   ├── AssetManager.java # Gerenciador de assets
│   │   └── TextureLoader.java# Carregamento de texturas
│   │
│   ├── audio/          # Áudio
│   │   ├── AudioManager.java # Gerenciador de áudio
│   │   ├── Sound.java        # Efeitos sonoros
│   │   └── Music.java        # Música de fundo
│   │
│   ├── tilemap/        # Mapas de tiles
│   │   ├── Tileset.java      # Conjunto de tiles
│   │   ├── Tilemap.java      # Mapa de tiles
│   │   └── TilemapRenderer.java # Renderizador
│   │
│   ├── physics/        # Física e colisão
│   │   ├── AABB.java         # Bounding box
│   │   ├── Raycast.java      # Raycasting
│   │   └── Collision.java    # Utilitários de colisão
│   │
│   ├── io/             # Entrada/Saída
│   │   └── SceneSerializer.java # Serialização
│   │
│   ├── math/           # Matemática
│   │   ├── Vector2.java      # Vetor 2D
│   │   └── Rectangle.java    # Retângulo
│   │
│   └── util/           # Utilitários
│       ├── Logger.java       # Sistema de log
│       └── RandomUtils.java  # Aleatoriedade
│
└── game/
    └── test/           # Jogo de exemplo
        ├── Main.java         # Ponto de entrada
        └── TestScene.java    # Cena de demonstração
```

## 🚀 Como Usar

### Build e testes reproduzíveis

Java 21 LTS é a toolchain fixa. O Gradle Wrapper 9.6.1 é a única entrada
canônica do build e baixa a distribuição fixada quando necessário.

No Windows:

```batch
gradlew.bat clean test
gradlew.bat projects
```

No Linux/macOS:

```bash
./gradlew clean test
./gradlew projects
```

`build.bat` e `build.sh` continuam como aliases de compatibilidade para
`clean test`; eles não mantêm listas manuais de fontes.

### Demo Java2D transitória

O Java2D atual ainda não representa o backend da futura 1.0.0. Durante a
migração, a demo é executada pelo classpath produzido pelo source set `legacy`:

```batch
gradlew.bat legacyDemo
```

```bash
./gradlew legacyDemo
```

O smoke autoencerrável usado na validação é `legacyDemoSmoke`. Os aliases
`run.bat` e `run.sh` continuam disponíveis até a remoção formal do boundary
legado.

### Módulos Gradle

- `engine:core`: classes já independentes de AWT, libGDX e LWJGL;
- `engine:gdx`: boundary vazio reservado ao spike da Issue #14;
- `desktop`: backend Java2D atual no source set transitório `legacy`;
- `game`: demo consumidora e seu ponto de entrada;
- raiz: somente agregação; não produz JAR monolítico.

### Controles do Demo

- **WASD / Setas**: Mover
- **SHIFT**: Correr
- **ESC**: Sair

## 💻 Exemplo Mínimo

```java
import engine.core.Game;
import engine.graphics.Renderer;
import engine.graphics.ColorPalette;

public class MeuJogo extends Game {
    
    public MeuJogo() {
        super("Meu Jogo", 800, 600);
    }
    
    @Override
    protected void onCreate() {
        // Inicialização
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        // Lógica do jogo
    }
    
    @Override
    protected void onRender(Renderer renderer) {
        renderer.clear(ColorPalette.PICO8_BLACK);
        renderer.fillRect(100, 100, 50, 50, ColorPalette.PICO8_RED);
    }
    
    public static void main(String[] args) {
        new MeuJogo().start();
    }
}
```

## 📐 Arquitetura

### Princípios

1. **Baixo Acoplamento** - Módulos independentes
2. **Alta Coesão** - Cada classe tem uma responsabilidade
3. **API Clara** - Engine nunca depende do jogo
4. **Core independente de backend** - Java2D permanece legado até o spike de libGDX/LWJGL3
5. **Pixel-Perfect** - Otimizado para pixel art

### Contratos de fundação

- Desktop suportado: Windows, Linux e macOS.
- Runtime baseline: Java 21 LTS; compatibilidade adicional em Java 25 LTS.
- Viewport virtual provisório: 320×180, nearest-neighbor e escala inteira.
- API estável futura: `engine.api.*`.
- Implementação interna: `engine.internal.*`.
- APIs experimentais: `engine.incubator.*`.

Os pacotes atuais em `engine.*` continuam sendo protótipo até a classificação da
Issue #13. Consulte [docs/versioning.md](docs/versioning.md).

### Fluxo Principal

```
Game.start()
    └── init()
        ├── Window (cria janela)
        ├── Renderer (inicializa gráficos)
        ├── Input (registra listeners)
        └── onCreate() [callback do usuário]
    
    └── run() [game loop]
        ├── Timer.update()
        ├── Input.update()
        ├── SceneManager.update(deltaTime)
        ├── onUpdate(deltaTime) [callback]
        ├── Renderer.begin()
        ├── SceneManager.render(renderer)
        ├── onRender(renderer) [callback]
        └── Renderer.end()
```

## 🎨 Paletas de Cores

```java
// PICO-8 (16 cores)
ColorPalette.PICO8_BLACK
ColorPalette.PICO8_RED
ColorPalette.PICO8_GREEN
// ...

// GameBoy (4 cores)
ColorPalette.GAMEBOY

// NES (12 cores)
ColorPalette.NES

// CGA (16 cores)
ColorPalette.CGA
```

## 🔧 Requisitos

- Java 21 LTS
- Java 25 LTS é uma linha adicional de compatibilidade, não a baseline
- Windows, Linux ou macOS
- Nenhuma dependência externa

## 📈 Evolução Futura

- [x] ~~Sistema de áudio~~ ✅ prototype-2.0.0
- [x] ~~Tilemap renderer~~ ✅ prototype-2.0.0
- [x] ~~Sistema de colisão~~ ✅ prototype-2.0.0
- [x] ~~Partículas simples~~ ✅ prototype-2.0.0
- [x] ~~Serialização de cenas~~ ✅ prototype-2.0.0
- [ ] Tween/Easing library
- [ ] Sistema de UI
- [ ] Pathfinding
- [ ] Suporte a gamepad

## 📄 Licença

A licença Apache-2.0 foi aprovada para o código na
[ADR-004](docs/adr/ADR-004-license-and-assets.md). O arquivo `LICENSE`, notices e
inventário de assets serão materializados pela Issue #13; até lá, o protótipo
não deve ser tratado como uma release licenciada.
