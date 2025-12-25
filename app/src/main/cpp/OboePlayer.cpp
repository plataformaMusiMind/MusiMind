/**
 * OboePlayer.cpp
 * 
 * Implementation of Oboe-based audio player.
 */

#include "OboePlayer.h"
#include <android/log.h>

#define LOG_TAG "OboePlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

OboePlayer::OboePlayer() {
    LOGI("OboePlayer created");
}

OboePlayer::~OboePlayer() {
    stop();
    LOGI("OboePlayer destroyed");
}

bool OboePlayer::start() {
    if (m_isRunning) {
        return true;
    }
    
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(m_channelCount);
    builder.setSampleRate(m_sampleRate);
    builder.setDataCallback(this);
    builder.setErrorCallback(this);
    builder.setUsage(oboe::Usage::Media);
    builder.setContentType(oboe::ContentType::Music);
    
    oboe::Result result = builder.openStream(m_stream);
    
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return false;
    }
    
    // Get actual sample rate
    m_sampleRate = m_stream->getSampleRate();
    m_engine.setSampleRate(m_sampleRate);
    
    LOGI("Stream opened: sampleRate=%d, channelCount=%d, framesPerBurst=%d",
         m_sampleRate,
         m_stream->getChannelCount(),
         m_stream->getFramesPerBurst());
    
    result = m_stream->requestStart();
    
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        m_stream->close();
        m_stream.reset();
        return false;
    }
    
    m_isRunning = true;
    LOGI("Audio stream started successfully");
    return true;
}

void OboePlayer::stop() {
    if (m_stream) {
        m_stream->requestStop();
        m_stream->close();
        m_stream.reset();
    }
    m_isRunning = false;
    LOGI("Audio stream stopped");
}

oboe::DataCallbackResult OboePlayer::onAudioReady(
    oboe::AudioStream* stream,
    void* audioData,
    int32_t numFrames
) {
    auto* output = static_cast<float*>(audioData);
    
    // Render audio from SoundFontEngine
    m_engine.render(output, numFrames);
    
    return oboe::DataCallbackResult::Continue;
}

void OboePlayer::onErrorAfterClose(
    oboe::AudioStream* stream,
    oboe::Result error
) {
    LOGE("Audio stream error: %s", oboe::convertToText(error));
    
    // Try to reopen stream
    if (m_isRunning) {
        reopenStream();
    }
}

void OboePlayer::reopenStream() {
    LOGI("Attempting to reopen stream...");
    stop();
    start();
}
