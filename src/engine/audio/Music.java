package engine.audio;

import engine.util.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Musica de fundo com streaming.
 * Para arquivos longos que nao devem ficar na memoria.
 */
public class Music implements Runnable {
    
    private String path;
    private Thread playThread;
    private SourceDataLine line;
    private AudioInputStream audioStream;
    
    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private volatile boolean loop = true;
    private volatile boolean stopped = false;
    
    private float volume = 1.0f;
    private FloatControl volumeControl;
    
    public Music(String path) {
        this.path = path;
    }
    
    // ==================== PLAYBACK ====================
    
    /**
     * Inicia a reproducao em thread separada.
     */
    public void play() {
        if (playing) {
            stop();
        }
        
        stopped = false;
        paused = false;
        playing = true;
        
        playThread = new Thread(this, "MusicThread");
        playThread.setDaemon(true);
        playThread.start();
    }
    
    @Override
    public void run() {
        try {
            do {
                // Abre stream
                File file = new File(path);
                if (file.exists()) {
                    audioStream = AudioSystem.getAudioInputStream(file);
                } else {
                    var is = getClass().getResourceAsStream("/" + path);
                    if (is == null) is = getClass().getResourceAsStream(path);
                    if (is == null) {
                        Logger.error("Musica nao encontrada: %s", path);
                        return;
                    }
                    audioStream = AudioSystem.getAudioInputStream(is);
                }
                
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                
                // Controle de volume
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    applyVolume();
                }
                
                line.start();
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while (!stopped && (bytesRead = audioStream.read(buffer)) != -1) {
                    // Pause handling
                    while (paused && !stopped) {
                        Thread.sleep(50);
                    }
                    
                    if (!stopped) {
                        line.write(buffer, 0, bytesRead);
                    }
                }
                
                line.drain();
                line.close();
                audioStream.close();
                
            } while (loop && !stopped);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
            Logger.error("Erro ao reproduzir musica: " + path, e);
        } finally {
            playing = false;
        }
    }
    
    /**
     * Para a reproducao.
     */
    public void stop() {
        stopped = true;
        paused = false;
        
        if (line != null) {
            line.stop();
            line.close();
        }
        
        if (playThread != null) {
            playThread.interrupt();
            try {
                playThread.join(500);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        playing = false;
    }
    
    /**
     * Pausa a reproducao.
     */
    public void pause() {
        paused = true;
    }
    
    /**
     * Continua apos pausa.
     */
    public void resume() {
        paused = false;
    }
    
    // ==================== ESTADO ====================
    
    public boolean isPlaying() {
        return playing && !paused;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public boolean isStopped() {
        return !playing;
    }
    
    // ==================== VOLUME ====================
    
    public void setVolume(float volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        applyVolume();
    }
    
    private void applyVolume() {
        if (volumeControl != null) {
            float dB = (float) (Math.log10(Math.max(0.0001, volume)) * 20);
            dB = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), dB));
            volumeControl.setValue(dB);
        }
    }
    
    public float getVolume() {
        return volume;
    }
    
    // ==================== LOOP ====================
    
    public void setLoop(boolean loop) {
        this.loop = loop;
    }
    
    public boolean isLoop() {
        return loop;
    }
    
    // ==================== LIFECYCLE ====================
    
    public void dispose() {
        stop();
    }
    
    public String getPath() {
        return path;
    }
    
    @Override
    public String toString() {
        return String.format("Music[%s, playing=%b]", path, playing);
    }
}

