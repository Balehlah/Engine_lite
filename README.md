# 🎮 Engine Lite

Engine 2D profissional em Java puro para jogos pixel art. Arquitetura modular, limpa e extensível.

## Estado do projeto

O código atual é um protótipo pré-1.0. libGDX/LWJGL3 foi aceito pela ADR-002
como backend desktop da futura 1.0.0, enquanto a demo consumidora ainda usa
Java2D como fallback legado. A decisão do backend não antecipa a migração
horizontal. O produto é o motor; `game.test` é apenas uma demo consumidora. A
futura versão 1.0.0 será a primeira release pública com contrato SemVer e API
estável auditada.

A versão de desenvolvimento é `1.0.0-SNAPSHOT`. Somente `engine.api.*` é
protegido pela baseline; todos os demais pacotes públicos atuais continuam
protótipo, internos, incubadores ou demo conforme
[docs/public-api.md](docs/public-api.md).

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
gradlew.bat verifyDistribution
```

No Linux:

```bash
./gradlew clean test
./gradlew projects
./gradlew verifyDistribution
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

O smoke autoencerrável da demo Java2D é `legacyDemoSmoke`; ele não valida o
spike. Os aliases `run.bat` e `run.sh` continuam disponíveis até a remoção
formal do boundary legado.

### Spike empacotado libGDX/LWJGL3

O spike removível da Issue #14 concluiu o gate e aprovou libGDX/LWJGL3 como
backend desktop. Ele constrói um ZIP canônico, extrai esse ZIP em diretório
temporário, inicia o launcher a partir de um CWD externo e autoencerra depois
de validar lifecycle, viewport, input, assets, áudio, Tiled e descarte de
recursos:

```batch
gradlew.bat buildSpikeDistribution verifyDistribution
gradlew.bat smokeSpikeDistribution -PspikeSmokeVariant=local-java21
```

```bash
./gradlew buildSpikeDistribution verifyDistribution
./gradlew smokeSpikeDistribution -PspikeSmokeVariant=local-java21
```

O ZIP fica em `desktop/build/distributions/`. Logs, screenshots e o manifesto
SHA-256 ficam em `desktop/build/reports/spike/`. A CI repete o smoke do mesmo
pacote em Java 21 e 25 nos runners Windows e Linux. O runner Windows usa Mesa
llvmpipe fixado e verificado somente durante o job; esses DLLs não entram no
ZIP. Consulte a
[especificação do spike](docs/spikes/libgdx-lwjgl3.md) e o
[registro de validação](docs/validation/issue-14.md).

### Módulos Gradle

- `engine:core`: classes já independentes de AWT, libGDX e LWJGL;
- `engine:gdx`: implementação incubadora e assets internos do spike da Issue
  #14, sem exposição pela API estável;
- `desktop`: launcher LWJGL3 do spike e backend Java2D no source set transitório
  `legacy`;
- `game`: demo consumidora e seu ponto de entrada;
- raiz: somente agregação; não produz JAR monolítico.

`clean test` também verifica a baseline de API, dependências/licenças,
atribuição de assets e o conteúdo dos JARs. Os relatórios são gerados em
`build/reports/licenses`, `build/reports/jars` e
`engine/core/build/reports/api`.

### Runtime determinístico incubador

O runtime backend-neutral em `engine.incubator.runtime.time` usa clock
monotônico injetável, acumulador puro e política imutável escolhida na
inicialização. A configuração padrão é 60 updates por segundo, clamp de 250 ms
por frame e no máximo cinco updates de catch-up. Cada update recebe exatamente
`1.0 / 60.0` segundo; o arredondamento do período do clock não altera o `dt`
lógico.

O adapter `FixedTimestepLoop` integra esse contrato ao callback de render do
libGDX: executa zero ou mais updates e sempre um render, com `alpha` no intervalo
`[0, 1)`. Clamp de wall time, descarte de backlog de simulação e tempo inativo
por pause ou `timeScale=0` possuem métricas separadas. Pause, resume, single-step
e time scale são expostos pelo adapter sem acumular tempo ocorrido enquanto a
simulação estava parada.

`SystemNanoClock` é o clock real; `FakeNanoClock` reproduz sequências sintéticas
sem espera. O spike mostra as métricas pelo `FixedTimestepDebugOverlay` em
execução interativa e registra a política e os totais em `timing.log` no smoke.
O overlay fica oculto durante a captura dos goldens para não alterar as fixtures
pixel-perfect. O backend Java2D continua como fallback legado e não foi migrado
por esta entrega.

### Input imutável por tick

O runtime incubador em `engine.incubator.runtime.input` separa callbacks do
backend do estado lógico. `InputEventQueue` preserva a ordem das bordas de
teclado/botão, scroll e foco em uma fila limitada a 4.096 eventos por padrão;
somente movimentos absolutos consecutivos do ponteiro podem ser coalescidos.
Saturação que não pode ser coalescida falha imediatamente e incrementa a
telemetria de overflow, sem descarte silencioso.

Cada update fixo consome um `InputSnapshot` imutável. `down` persiste entre
ticks, enquanto `pressed`, `released`, delta e scroll existem em exatamente um
tick. Press e release rápidos permanecem simultaneamente observáveis no mesmo
snapshot. Perda de foco libera teclas e botões mantidos e ignora callbacks de
press tardios até o foco retornar.

`ScreenToVirtual` aplica, no limite de cada tick, o tamanho lógico, backbuffer,
viewport inteiro e resolução virtual atuais. Isso cobre resize e DPI sem
capturar uma transformação obsoleta no callback, produz coordenadas virtuais
com origem inferior esquerda e distingue `VIEWPORT`, `BARS` e
`OUTSIDE_SURFACE`. O `GdxInputAdapter` somente traduz callbacks libGDX para essa
fila; a simulação do spike reage exclusivamente aos snapshots. `FakeInput`
reproduz scripts inteiramente em memória com a mesma sequência de snapshots.

### GameContext e lifecycle determinístico

O runtime incubador em `engine.incubator.runtime.lifecycle` cria um
`GameContext` novo para cada execução. O contexto contém somente estado local:
entidades, eventos, assets e ownership de recursos. Não existe instância
estática ou singleton nesse contrato.

`GameRuntime` executa cenas na ordem `create`, `enter`, `fixedUpdate`/`render`,
`exit` e `dispose`. Uma cena solicita a próxima por
`GameContext.requestScene`; pedidos feitos dentro de callbacks entram em uma
fila FIFO e só são aplicados depois que o callback atual termina. Exceções em
qualquer fase fecham a execução por `finally`, preservam a falha original e
acrescentam falhas de cleanup como suprimidas.

Entidades, eventos, assets e recursos declaram um único owner por identidade.
O owner é liberado de forma idempotente, recursos são descartados uma vez em
ordem inversa ao registro e `ResourceMetrics` expõe owners vivos, tentativas,
falhas e leaks. `restart` fecha integralmente o contexto anterior antes de
criar o seguinte. `GdxGameRuntimeLoop` conecta esse contrato ao scheduler fixo
do backend libGDX sem introduzir dependências gráficas em `engine:core`.

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
4. **Core independente de backend** - Java2D permanece legado até a decisão final da ADR-002
5. **Pixel-Perfect** - Otimizado para pixel art

### Contratos de fundação

- Desktop suportado: Windows e Linux.
- Runtime baseline: Java 21 LTS; compatibilidade adicional em Java 25 LTS.
- Viewport virtual provisório: 320×180, nearest-neighbor e escala inteira.
- API estável futura: `engine.api.*`.
- Implementação interna: `engine.internal.*`.
- APIs experimentais: `engine.incubator.*`.

Os pacotes atuais fora de `engine.api.*` continuam sendo protótipo legado,
internos ou incubadores e não integram a baseline. Consulte
[docs/versioning.md](docs/versioning.md) e
[docs/public-api.md](docs/public-api.md).

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
- Windows ou Linux
- libGDX 1.14.2 e LWJGL 3.4.1 são resolvidos pelo Gradle Wrapper somente para
  o backend empacotado

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

O código é distribuído sob
[Apache License 2.0](LICENSE), conforme a
[ADR-004](docs/adr/ADR-004-license-and-assets.md). Dependências e ferramentas
estão inventariadas em
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); assets só podem ser
distribuídos quando registrados em
[assets/ATTRIBUTION.md](assets/ATTRIBUTION.md).
