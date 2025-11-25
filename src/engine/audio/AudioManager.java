package engine.audio;

import engine.math.Vector2;
import engine.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador central de audio.
 * Suporta sons, musica e audio espacial.
 */
public final class AudioManager {
    
    private static AudioManager instance;
    
    // Cache de sons
    private final Map<String, Sound> sounds;
    
    // Musica atual
    private Music currentMusic;
    private String currentMusicKey;
    
    // Volumes globais
    private float masterVolume = 1.0f;
    private float soundVolume = 1.0f;
    private float musicVolume = 1.0f;
    
    // Audio espacial
    private Vector2 listenerPosition = Vector2.ZERO;
    private float maxDistance = 500f;
    
    // Estado
    private boolean muted = false;
    
    private AudioManager() {
        sounds = new HashMap<>();
    }
    
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    // ==================== SONS ====================
    
    /**
     * Carrega som no cache.
     */
    public Sound loadSound(String key, String path) {
        if (sounds.containsKey(key)) {
            return sounds.get(key);
        }
        
        Sound sound = new Sound(path);
        sounds.put(key, sound);
        return sound;
    }
    
    /**
     * Retorna som do cache.
     */
    public Sound getSound(String key) {
        return sounds.get(key);
    }
    
    /**
     * Reproduz som pelo key.
     */
    public void playSound(String key) {
        Sound sound = sounds.get(key);
        if (sound != null && !muted) {
            sound.setVolume(soundVolume * masterVolume);
            sound.play();
        }
    }
    
    /**
     * Reproduz som com volume customizado.
     */
    public void playSound(String key, float volume) {
        Sound sound = sounds.get(key);
        if (sound != null && !muted) {
            sound.setVolume(volume * soundVolume * masterVolume);
            sound.play();
        }
    }
    
    /**
     * Reproduz som com audio espacial.
     */
    public void playSoundAt(String key, Vector2 position) {
        playSoundAt(key, position, 1.0f);
    }
    
    /**
     * Reproduz som com audio espacial e volume base.
     */
    public void playSoundAt(String key, Vector2 position, float baseVolume) {
        Sound sound = sounds.get(key);
        if (sound == null || muted) return;
        
        // Calcula distancia
        float distance = listenerPosition.distance(position);
        
        // Volume baseado na distancia (linear falloff)
        float distanceVolume = 1.0f - Math.min(1.0f, distance / maxDistance);
        float finalVolume = baseVolume * distanceVolume * soundVolume * masterVolume;
        
        if (finalVolume <= 0.01f) return; // Som muito baixo, ignora
        
        // Pan baseado na posicao horizontal
        float dx = position.x - listenerPosition.x;
        float pan = Math.max(-1, Math.min(1, dx / (maxDistance * 0.5f)));
        
        sound.setVolume(finalVolume);
        sound.setPan(pan);
        sound.play();
    }
    
    /**
     * Para todos os sons.
     */
    public void stopAllSounds() {
        for (Sound sound : sounds.values()) {
            sound.stop();
        }
    }
    
    // ==================== MUSICA ====================
    
    /**
     * Toca musica.
     */
    public void playMusic(String path) {
        playMusic(path, true);
    }
    
    /**
     * Toca musica com opcao de loop.
     */
    public void playMusic(String path, boolean loop) {
        // Para musica anterior
        if (currentMusic != null) {
            currentMusic.stop();
        }
        
        currentMusicKey = path;
        currentMusic = new Music(path);
        currentMusic.setLoop(loop);
        currentMusic.setVolume(musicVolume * masterVolume);
        
        if (!muted) {
            currentMusic.play();
        }
    }
    
    /**
     * Para a musica atual.
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicKey = null;
        }
    }
    
    /**
     * Pausa a musica.
     */
    public void pauseMusic() {
        if (currentMusic != null) {
            currentMusic.pause();
        }
    }
    
    /**
     * Continua a musica.
     */
    public void resumeMusic() {
        if (currentMusic != null && !muted) {
            currentMusic.resume();
        }
    }
    
    /**
     * Verifica se tem musica tocando.
     */
    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }
    
    // ==================== VOLUME ====================
    
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolume();
    }
    
    public float getMasterVolume() {
        return masterVolume;
    }
    
    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0, Math.min(1, volume));
    }
    
    public float getSoundVolume() {
        return soundVolume;
    }
    
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolume();
    }
    
    public float getMusicVolume() {
        return musicVolume;
    }
    
    private void updateMusicVolume() {
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume * masterVolume);
        }
    }
    
    // ==================== MUTE ====================
    
    public void setMuted(boolean muted) {
        this.muted = muted;
        
        if (muted) {
            stopAllSounds();
            if (currentMusic != null) {
                currentMusic.pause();
            }
        } else {
            if (currentMusic != null) {
                currentMusic.resume();
            }
        }
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public void toggleMute() {
        setMuted(!muted);
    }
    
    // ==================== AUDIO ESPACIAL ====================
    
    /**
     * Define posicao do listener (geralmente a camera ou player).
     */
    public void setListenerPosition(Vector2 position) {
        this.listenerPosition = position;
    }
    
    public Vector2 getListenerPosition() {
        return listenerPosition;
    }
    
    /**
     * Define distancia maxima para audio espacial.
     */
    public void setMaxDistance(float distance) {
        this.maxDistance = distance;
    }
    
    public float getMaxDistance() {
        return maxDistance;
    }
    
    // ==================== GERENCIAMENTO ====================
    
    /**
     * Remove som do cache.
     */
    public void unloadSound(String key) {
        Sound sound = sounds.remove(key);
        if (sound != null) {
            sound.dispose();
        }
    }
    
    /**
     * Limpa todos os sons e para a musica.
     */
    public void clear() {
        stopAllSounds();
        stopMusic();
        
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        
        Logger.info("AudioManager limpo");
    }
    
    /**
     * Libera todos os recursos.
     */
    public void dispose() {
        clear();
    }
    
    // ==================== ESTATISTICAS ====================
    
    public int getSoundCount() {
        return sounds.size();
    }
    
    public String getCurrentMusicKey() {
        return currentMusicKey;
    }
}

