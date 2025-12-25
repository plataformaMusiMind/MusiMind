/**
 * OboePlayer.h
 * 
 * Oboe-based low-latency audio player.
 * Provides the audio callback that renders samples from SoundFontEngine.
 */

#ifndef MUSIMIND_OBOE_PLAYER_H
#define MUSIMIND_OBOE_PLAYER_H

#include <oboe/Oboe.h>
#include "SoundFontEngine.h"
#include <memory>

class OboePlayer : public oboe::AudioStreamDataCallback,
                   public oboe::AudioStreamErrorCallback {
public:
    OboePlayer();
    ~OboePlayer();
    
    // Initialize audio stream
    bool start();
    void stop();
    
    // Get SoundFontEngine
    SoundFontEngine& getSoundFontEngine() { return m_engine; }
    
    // Get sample rate
    int getSampleRate() const { return m_sampleRate; }
    
    // Oboe callbacks
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames
    ) override;
    
    void onErrorAfterClose(
        oboe::AudioStream* stream,
        oboe::Result error
    ) override;
    
private:
    void reopenStream();
    
    std::shared_ptr<oboe::AudioStream> m_stream;
    SoundFontEngine m_engine;
    int m_sampleRate = 48000;
    int m_channelCount = 2;
    bool m_isRunning = false;
};

#endif // MUSIMIND_OBOE_PLAYER_H
