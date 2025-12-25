/**
 * SoundFontEngine.cpp
 * 
 * Implementation of TinySoundFont-based synthesizer.
 * Uses gm.sf2 for piano and Metronom.sf2 for metronome clicks.
 */

// TinySoundFont - define implementation ONLY here (header-only library)
#define TSF_IMPLEMENTATION
#include "tsf.h"

#include "SoundFontEngine.h"
#include <android/log.h>
#include <cmath>
#include <cstring>

#define LOG_TAG "SoundFontEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Metronome MIDI notes for Metronom.sf2 (from MuseScore/Ardour)
// According to the SoundFont documentation:
// - E5 (MIDI 76) = "tick" - downbeat (first beat, accented)
// - F5 (MIDI 77) = "tack" - other beats (non-accented)
// Note: Only these two pitches produce sound!
constexpr int METRONOME_NOTE_ACCENTED = 76;   // E5 - "tick" for downbeat
constexpr int METRONOME_NOTE_NORMAL = 77;     // F5 - "tack" for other beats
constexpr float METRONOME_VELOCITY = 1.0f;

SoundFontEngine::SoundFontEngine() {
    LOGI("SoundFontEngine created");
}

SoundFontEngine::~SoundFontEngine() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_tsf) {
        tsf_close(m_tsf);
        m_tsf = nullptr;
    }
    if (m_tsfMetronome) {
        tsf_close(m_tsfMetronome);
        m_tsfMetronome = nullptr;
    }
    LOGI("SoundFontEngine destroyed");
}

tsf* SoundFontEngine::loadSoundFont(AAssetManager* assetManager, const char* path) {
    AAsset* asset = AAssetManager_open(assetManager, path, AASSET_MODE_BUFFER);
    if (!asset) {
        LOGE("Failed to open SoundFont asset: %s", path);
        return nullptr;
    }
    
    const void* data = AAsset_getBuffer(asset);
    off_t dataSize = AAsset_getLength(asset);
    
    if (!data || dataSize <= 0) {
        LOGE("Failed to get SoundFont data for: %s", path);
        AAsset_close(asset);
        return nullptr;
    }
    
    LOGI("Loading SoundFont: %s (%ld bytes)", path, (long)dataSize);
    
    tsf* soundfont = tsf_load_memory(data, (int)dataSize);
    AAsset_close(asset);
    
    if (!soundfont) {
        LOGE("Failed to parse SoundFont: %s", path);
        return nullptr;
    }
    
    LOGI("SoundFont loaded: %s, presets: %d", path, tsf_get_presetcount(soundfont));
    return soundfont;
}

bool SoundFontEngine::initialize(AAssetManager* assetManager, const char* pianoSfPath, const char* metronomeSfPath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_assetManager = assetManager;
    
    // Close existing instances
    if (m_tsf) {
        tsf_close(m_tsf);
        m_tsf = nullptr;
    }
    if (m_tsfMetronome) {
        tsf_close(m_tsfMetronome);
        m_tsfMetronome = nullptr;
    }
    
    // Load piano SoundFont
    m_tsf = loadSoundFont(assetManager, pianoSfPath);
    if (!m_tsf) {
        return false;
    }
    tsf_set_output(m_tsf, TSF_STEREO_INTERLEAVED, m_sampleRate, 0.0f);
    
    // Load metronome SoundFont
    m_tsfMetronome = loadSoundFont(assetManager, metronomeSfPath);
    if (m_tsfMetronome) {
        tsf_set_output(m_tsfMetronome, TSF_STEREO_INTERLEAVED, m_sampleRate, 0.0f);
        LOGI("Metronome SoundFont loaded successfully");
    } else {
        LOGE("Failed to load metronome SoundFont, will use synthetic fallback");
    }
    
    LOGI("SoundFontEngine initialized with piano and metronome SoundFonts");
    return true;
}

bool SoundFontEngine::initialize(AAssetManager* assetManager, const char* sfPath) {
    // Legacy call - try to load both piano and metronome
    return initialize(assetManager, sfPath, "soundfonts/Metronom.sf2");
}

void SoundFontEngine::setSampleRate(int sampleRate) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_sampleRate = sampleRate;
    if (m_tsf) {
        tsf_set_output(m_tsf, TSF_STEREO_INTERLEAVED, sampleRate, 0.0f);
    }
    if (m_tsfMetronome) {
        tsf_set_output(m_tsfMetronome, TSF_STEREO_INTERLEAVED, sampleRate, 0.0f);
    }
}

void SoundFontEngine::noteOn(int channel, int midiNote, float velocity) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_tsf) {
        LOGI("Note ON: channel=%d, note=%d, velocity=%.2f", channel, midiNote, velocity);
        // Use preset 0 (Grand Piano) for all notes
        tsf_note_on(m_tsf, 0, midiNote, velocity);
    }
}

void SoundFontEngine::noteOff(int channel, int midiNote) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_tsf) {
        tsf_note_off(m_tsf, 0, midiNote);
    }
}

void SoundFontEngine::setPreset(int channel, int preset) {
    LOGI("Set preset: channel=%d, preset=%d", channel, preset);
}

const char* SoundFontEngine::getPresetName(int preset) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_tsf) {
        return tsf_get_presetname(m_tsf, preset);
    }
    return "Unknown";
}

void SoundFontEngine::playMetronomeClick(bool isAccented) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_tsfMetronome) {
        // Use the metronome SoundFont - trigger a note
        int note = isAccented ? METRONOME_NOTE_ACCENTED : METRONOME_NOTE_NORMAL;
        float velocity = isAccented ? 1.0f : 0.8f;
        
        // Turn off any previous note quickly and start new one
        tsf_note_off(m_tsfMetronome, 0, METRONOME_NOTE_NORMAL);
        tsf_note_off(m_tsfMetronome, 0, METRONOME_NOTE_ACCENTED);
        tsf_note_on(m_tsfMetronome, 0, note, velocity);
        
        LOGI("Metronome click (SoundFont): note=%d, accented=%d", note, isAccented);
    } else {
        // Fallback - shouldn't happen but just in case
        LOGE("Metronome SoundFont not loaded!");
    }
}

void SoundFontEngine::render(float* output, int numFrames) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    // Clear output buffer
    memset(output, 0, numFrames * 2 * sizeof(float));
    
    // Render piano SoundFont
    if (m_tsf) {
        tsf_render_float(m_tsf, output, numFrames, 0);
    }
    
    // Mix in metronome SoundFont
    if (m_tsfMetronome) {
        // Render metronome to temp buffer and mix
        std::vector<float> metronomeBuffer(numFrames * 2, 0.0f);
        tsf_render_float(m_tsfMetronome, metronomeBuffer.data(), numFrames, 0);
        
        // Mix metronome into output
        for (int i = 0; i < numFrames * 2; i++) {
            output[i] += metronomeBuffer[i];
        }
    }
}
