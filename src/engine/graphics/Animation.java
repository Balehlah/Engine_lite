package engine.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Animação baseada em frames de sprite.
 * Suporta loop, pingpong, eventos de conclusão.
 */
public class Animation {
    
    public enum PlayMode {
        NORMAL,       // Reproduz uma vez
        LOOP,         // Repete infinitamente
        LOOP_PINGPONG // Vai e volta
    }
    
    // Frames
    private final Sprite[] frames;
    private final float frameDuration;
    
    // Estado
    private float time;
    private int currentFrame;
    private boolean playing;
    private boolean finished;
    private PlayMode playMode;
    private boolean reversed;
    
    // Callbacks
    private Runnable onComplete;
    private Runnable onLoop;
    
    /**
     * Cria animação com frames e duração por frame.
     */
    public Animation(Sprite[] frames, float frameDuration) {
        this.frames = frames;
        this.frameDuration = frameDuration;
        this.playMode = PlayMode.LOOP;
        this.playing = true;
        reset();
    }
    
    /**
     * Cria animação a partir de spritesheet.
     */
    public Animation(BufferedImage spritesheet, int frameWidth, int frameHeight, float frameDuration) {
        this(Sprite.fromSpritesheet(spritesheet, frameWidth, frameHeight), frameDuration);
    }
    
    /**
     * Cria animação com subset de frames de um spritesheet.
     */
    public Animation(BufferedImage spritesheet, int frameWidth, int frameHeight, 
                     int startFrame, int endFrame, float frameDuration) {
        Sprite[] allFrames = Sprite.fromSpritesheet(spritesheet, frameWidth, frameHeight);
        int count = endFrame - startFrame + 1;
        Sprite[] subset = new Sprite[count];
        System.arraycopy(allFrames, startFrame, subset, 0, count);
        
        this.frames = subset;
        this.frameDuration = frameDuration;
        this.playMode = PlayMode.LOOP;
        this.playing = true;
        reset();
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Atualiza a animação.
     */
    public void update(float deltaTime) {
        if (!playing || finished) return;
        
        time += deltaTime;
        
        float totalDuration = frameDuration * frames.length;
        
        if (time >= frameDuration) {
            time -= frameDuration;
            advanceFrame();
        }
    }
    
    private void advanceFrame() {
        switch (playMode) {
            case NORMAL:
                if (currentFrame < frames.length - 1) {
                    currentFrame++;
                } else {
                    finished = true;
                    playing = false;
                    if (onComplete != null) onComplete.run();
                }
                break;
                
            case LOOP:
                currentFrame = (currentFrame + 1) % frames.length;
                if (currentFrame == 0 && onLoop != null) {
                    onLoop.run();
                }
                break;
                
            case LOOP_PINGPONG:
                if (!reversed) {
                    if (currentFrame < frames.length - 1) {
                        currentFrame++;
                    } else {
                        reversed = true;
                        currentFrame--;
                        if (onLoop != null) onLoop.run();
                    }
                } else {
                    if (currentFrame > 0) {
                        currentFrame--;
                    } else {
                        reversed = false;
                        currentFrame++;
                        if (onLoop != null) onLoop.run();
                    }
                }
                break;
        }
    }
    
    // ==================== CONTROLE ====================
    
    public void play() {
        playing = true;
    }
    
    public void pause() {
        playing = false;
    }
    
    public void stop() {
        playing = false;
        reset();
    }
    
    public void reset() {
        time = 0;
        currentFrame = 0;
        finished = false;
        reversed = false;
    }
    
    public void restart() {
        reset();
        play();
    }
    
    // ==================== RENDERING ====================
    
    /**
     * Desenha o frame atual.
     */
    public void draw(Graphics2D g, float x, float y) {
        getCurrentSprite().draw(g, x, y);
    }
    
    /**
     * Retorna o sprite do frame atual.
     */
    public Sprite getCurrentSprite() {
        return frames[currentFrame];
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public int getCurrentFrame() {
        return currentFrame;
    }
    
    public void setCurrentFrame(int frame) {
        this.currentFrame = Math.max(0, Math.min(frame, frames.length - 1));
    }
    
    public int getFrameCount() {
        return frames.length;
    }
    
    public Sprite[] getFrames() {
        return frames;
    }
    
    public float getFrameDuration() {
        return frameDuration;
    }
    
    public float getTotalDuration() {
        return frameDuration * frames.length;
    }
    
    public float getTime() {
        return time;
    }
    
    public boolean isPlaying() {
        return playing;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public PlayMode getPlayMode() {
        return playMode;
    }
    
    public void setPlayMode(PlayMode mode) {
        this.playMode = mode;
    }
    
    // ==================== CALLBACKS ====================
    
    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }
    
    public void setOnLoop(Runnable callback) {
        this.onLoop = callback;
    }
    
    // ==================== PROPRIEDADES DO SPRITE ====================
    
    public int getWidth() {
        return getCurrentSprite().getWidth();
    }
    
    public int getHeight() {
        return getCurrentSprite().getHeight();
    }
    
    public void setScale(float scale) {
        for (Sprite frame : frames) {
            frame.setScale(scale);
        }
    }
    
    public void setScale(float x, float y) {
        for (Sprite frame : frames) {
            frame.setScale(x, y);
        }
    }
    
    public void setOrigin(float x, float y) {
        for (Sprite frame : frames) {
            frame.setOrigin(x, y);
        }
    }
    
    public void setFlipX(boolean flip) {
        for (Sprite frame : frames) {
            frame.setFlipX(flip);
        }
    }
    
    public void setFlipY(boolean flip) {
        for (Sprite frame : frames) {
            frame.setFlipY(flip);
        }
    }
    
    public void setAlpha(float alpha) {
        for (Sprite frame : frames) {
            frame.setAlpha(alpha);
        }
    }
    
    // ==================== FACTORY ====================
    
    /**
     * Cria animação a partir de linha específica de um spritesheet.
     */
    public static Animation fromRow(BufferedImage spritesheet, int frameWidth, int frameHeight, 
                                    int row, int frameCount, float frameDuration) {
        Sprite[] frames = new Sprite[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = Sprite.fromSpritesheet(spritesheet, frameWidth, frameHeight, i, row);
        }
        return new Animation(frames, frameDuration);
    }
    
    /**
     * Cria animação a partir de coluna específica de um spritesheet.
     */
    public static Animation fromColumn(BufferedImage spritesheet, int frameWidth, int frameHeight,
                                       int col, int frameCount, float frameDuration) {
        Sprite[] frames = new Sprite[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = Sprite.fromSpritesheet(spritesheet, frameWidth, frameHeight, col, i);
        }
        return new Animation(frames, frameDuration);
    }
}

