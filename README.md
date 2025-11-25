# 🎮 Pixel Engine Gabriel 2025

Engine 2D profissional em Java puro para jogos pixel art. Arquitetura modular, limpa e extensível.

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
│   │   └── DrawUtils.java    # Primitivas
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

### Build & Run (Windows)

```batch
build.bat
run.bat
```

### Build & Run (Linux/Mac)

```bash
chmod +x build.sh run.sh
./build.sh
./run.sh
```

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
4. **Zero Dependências** - Apenas Java padrão (Java2D)
5. **Pixel-Perfect** - Otimizado para pixel art

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

- Java 11+ (recomendado Java 17+)
- Nenhuma dependência externa

## 📈 Evolução Futura

- [ ] Sistema de áudio
- [ ] Tilemap renderer
- [ ] Sistema de colisão
- [ ] Partículas simples
- [ ] Serialização de cenas
- [ ] Tween/Easing library

## 📄 Licença

Projeto pessoal de Gabriel - 2025
