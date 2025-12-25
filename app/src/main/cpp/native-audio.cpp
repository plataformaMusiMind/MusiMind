/**
 * native-audio.cpp
 * 
 * JNI bridge between Kotlin and native audio engine.
 * Provides functions for initialization, note playback, and metronome.
 */

#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "OboePlayer.h"
#include <memory>

#define LOG_TAG "NativeAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global player instance
static std::unique_ptr<OboePlayer> g_player;

extern "C" {

/**
 * Initialize the audio engine with the SoundFont file.
 */
JNIEXPORT jboolean JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeInitialize(
    JNIEnv* env,
    jobject /* this */,
    jobject assetManager,
    jstring soundFontPath
) {
    LOGI("Initializing native audio engine...");
    
    // Get asset manager
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!mgr) {
        LOGE("Failed to get AssetManager");
        return JNI_FALSE;
    }
    
    // Get SoundFont path
    const char* sfPath = env->GetStringUTFChars(soundFontPath, nullptr);
    if (!sfPath) {
        LOGE("Failed to get SoundFont path");
        return JNI_FALSE;
    }
    
    // Create player
    g_player = std::make_unique<OboePlayer>();
    
    // Initialize SoundFont engine
    bool sfLoaded = g_player->getSoundFontEngine().initialize(mgr, sfPath);
    env->ReleaseStringUTFChars(soundFontPath, sfPath);
    
    if (!sfLoaded) {
        LOGE("Failed to load SoundFont");
        g_player.reset();
        return JNI_FALSE;
    }
    
    // Start audio stream
    if (!g_player->start()) {
        LOGE("Failed to start audio stream");
        g_player.reset();
        return JNI_FALSE;
    }
    
    LOGI("Native audio engine initialized successfully");
    return JNI_TRUE;
}

/**
 * Play a MIDI note.
 */
JNIEXPORT void JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeNoteOn(
    JNIEnv* env,
    jobject /* this */,
    jint channel,
    jint midiNote,
    jfloat velocity
) {
    if (g_player) {
        g_player->getSoundFontEngine().noteOn(channel, midiNote, velocity);
    }
}

/**
 * Stop a MIDI note.
 */
JNIEXPORT void JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeNoteOff(
    JNIEnv* env,
    jobject /* this */,
    jint channel,
    jint midiNote
) {
    if (g_player) {
        g_player->getSoundFontEngine().noteOff(channel, midiNote);
    }
}

/**
 * Play a metronome click.
 */
JNIEXPORT void JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativePlayMetronome(
    JNIEnv* env,
    jobject /* this */,
    jboolean isAccented
) {
    if (g_player) {
        g_player->getSoundFontEngine().playMetronomeClick(isAccented);
    }
}

/**
 * Set the instrument preset for a channel.
 */
JNIEXPORT void JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeSetPreset(
    JNIEnv* env,
    jobject /* this */,
    jint channel,
    jint preset
) {
    if (g_player) {
        g_player->getSoundFontEngine().setPreset(channel, preset);
    }
}

/**
 * Check if the engine is ready.
 */
JNIEXPORT jboolean JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeIsReady(
    JNIEnv* env,
    jobject /* this */
) {
    return g_player && g_player->getSoundFontEngine().isLoaded() ? JNI_TRUE : JNI_FALSE;
}

/**
 * Release all resources.
 */
JNIEXPORT void JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeRelease(
    JNIEnv* env,
    jobject /* this */
) {
    LOGI("Releasing native audio engine...");
    g_player.reset();
    LOGI("Native audio engine released");
}

/**
 * Get the sample rate.
 */
JNIEXPORT jint JNICALL
Java_com_musimind_music_audio_nativeaudio_NativeAudioBridge_nativeGetSampleRate(
    JNIEnv* env,
    jobject /* this */
) {
    return g_player ? g_player->getSampleRate() : 44100;
}

} // extern "C"
