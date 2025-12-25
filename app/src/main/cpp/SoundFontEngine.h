/**
 * SoundFontEngine.h
 * 
 * TinySoundFont-based synthesizer for high-quality piano sounds.
 * Uses gm.sf2 for piano and Metronom.sf2 for metronome clicks.
 */

#ifndef MUSIMIND_SOUNDFONT_ENGINE_H
#define MUSIMIND_SOUNDFONT_ENGINE_H

#include <android/asset_manager.h>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>

// Forward declare tsf type (implementation in .cpp)
struct tsf;

class SoundFontEngine {
public:
    SoundFontEngine();
    ~SoundFontEngine();
    
    // Initialize with Android asset manager - loads both SoundFonts
    bool initialize(AAssetManager* assetManager, const char* pianoSfPath, const char* metronomeSfPath);
    
    // Legacy single-file initialize (for backwards compatibility)
    bool initialize(AAssetManager* assetManager, const char* sfPath);
    
    // Play a MIDI note (piano)
    void noteOn(int channel, int midiNote, float velocity);
    void noteOff(int channel, int midiNote);
    
    // Render audio samples (called by Oboe callback)
    void render(float* output, int numFrames);
    
    // Set output sample rate
    void setSampleRate(int sampleRate);
    
    // Get preset name
    const char* getPresetName(int preset);
    
    // Set preset (instrument)
    void setPreset(int channel, int preset);
    
    // Play metronome click using Metronom.sf2
    void playMetronomeClick(bool isAccented);
    
    // Check if loaded
    bool isLoaded() const { return m_tsf != nullptr; }
    bool isMetronomeLoaded() const { return m_tsfMetronome != nullptr; }
    
private:
    // Load a SoundFont from assets
    tsf* loadSoundFont(AAssetManager* assetManager, const char* path);
    
    tsf* m_tsf = nullptr;           // Piano SoundFont
    tsf* m_tsfMetronome = nullptr;  // Metronome SoundFont
    AAssetManager* m_assetManager = nullptr;
    std::mutex m_mutex;
    int m_sampleRate = 44100;
    
    // Metronome state
    std::atomic<bool> m_metronomeClick{false};
    std::atomic<bool> m_metronomeAccented{false};
};

#endif // MUSIMIND_SOUNDFONT_ENGINE_H
