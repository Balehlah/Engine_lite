package engine.audio;

import engine.util.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Efeito sonoro curto.
 * Carregado completamente na memoria para playback instantaneo.
 */
public class Sound {
    
    private Clip clip;
    private FloatControl volumeControl;
    private FloatControl panControl;
    
    private float volume = 1.0f;
    private float pan = 0.0f;
    private boolean loop = false;
    
    private final String name;
    
    /**
     * Carrega som de um arquivo.
     */
    public Sound(String path) {
        this.name = path;
        load(path);
    }
    
    /**
     * Carrega som de um InputStream.
     */
    public Sound(String name, InputStream stream) {
        this.name = name;
        loadFromStream(stream);
    }
    
    private void load(String path) {
        try {
            File file = new File(path);
            AudioInputStream audioStream;
            
            if (file.exists()) {
                audioStream = AudioSystem.getAudioInputStream(file);
            } else {
                InputStream is = getClass().getResourceAsStream("/" + path);
                if (is == null) {
                    is = getClass().getResourceAsStream(path);
                }
                if (is == null) {
                    Logger.error("Som nao encontrado: %s", path);
                    return;
                }
                audioStream = AudioSystem.getAudioInputStream(is);
            }
            
            initClip(audioStream);
            Logger.debug("Som carregado: %s", path);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            Logger.error("Erro ao carregar som: " + path, e);
        }
    }
    
    private void loadFromStream(InputStream stream) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(stream);
            initClip(audioStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            Logger.error("Erro ao carregar som de stream", e);
        }
    }
    
    private void initClip(AudioInputStream audioStream) throws LineUnavailableException, IOException {
        clip = AudioSystem.getClip();
        clip.open(audioStream);
        
        // Controles de volume e pan
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        }
        if (clip.isControlSupported(FloatControl.Type.PAN)) {
            panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
        }
    }
    
    // ==================== PLAYBACK ====================
    
    /**
     * Reproduz o som do inicio.
     */
    public void play() {
        if (clip == null) return;
        
        stop();
        clip.setFramePosition(0);
        
        if (loop) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.start();
        }
    }
    
    /**
     * Para a reproducao.
     */
    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
    
    /**
     * Pausa a reproducao.
     */
    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
    
    /**
     * Continua a reproducao.
     */
    public void resume() {
        if (clip != null && !clip.isRunning()) {
            clip.start();
        }
    }
    
    /**
     * Verifica se esta tocando.
     */
    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
    
    // ==================== VOLUME ====================
    
    /**
     * Define volume (0.0 a 1.0).
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        
        if (volumeControl != null) {
            // Converte para dB
            float dB = (float) (Math.log10(Math.max(0.0001, this.volume)) * 20);
            dB = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), dB));
            volumeControl.setValue(dB);
        }
    }
    
    public float getVolume() {
        return volume;
    }
    
    // ==================== PAN (Stereo) ====================
    
    /**
     * Define pan (-1.0 esquerda, 0.0 centro, 1.0 direita).
     */
    public void setPan(float pan) {
        this.pan = Math.max(-1, Math.min(1, pan));
        
        if (panControl != null) {
            panControl.setValue(this.pan);
        }
    }
    
    public float getPan() {
        return pan;
    }
    
    // ==================== LOOP ====================
    
    public void setLoop(boolean loop) {
        this.loop = loop;
    }
    
    public boolean isLoop() {
        return loop;
    }
    
    // ==================== DURACAO ====================
    
    /**
     * Duracao em segundos.
     */
    public float getDuration() {
        if (clip == null) return 0;
        return clip.getMicrosecondLength() / 1_000_000f;
    }
    
    /**
     * Posicao atual em segundos.
     */
    public float getPosition() {
        if (clip == null) return 0;
        return clip.getMicrosecondPosition() / 1_000_000f;
    }
    
    /**
     * Define posicao em segundos.
     */
    public void setPosition(float seconds) {
        if (clip != null) {
            clip.setMicrosecondPosition((long) (seconds * 1_000_000));
        }
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Libera recursos.
     */
    public void dispose() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return String.format("Sound[%s, %.2fs]", name, getDuration());
    }
}

