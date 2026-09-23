#pragma once

// Modified native MediaPlayerInfo bridge for LoVisual by loki5512344.
// Based on Redstonecrafter0/MediaPlayerInfo.

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaPlayerInfo_getMediaSessions(JNIEnv* env, jobject obj);
JNIEXPORT jint JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaPlayerInfo_fillSessionSnapshotBuffer(JNIEnv* env, jobject obj, jobject buffer, jint maxSessions);
JNIEXPORT jbyteArray JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaPlayerInfo_getArtworkPng(JNIEnv* env, jobject obj, jstring sessionId);

JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_play(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_pause(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_playPause(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_stop(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_next(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_previous(JNIEnv* env, jobject obj);

JNIEXPORT jboolean JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSupportsShuffle(JNIEnv* env, jobject obj);
JNIEXPORT jboolean JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeIsShuffleActive(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSetShuffle(JNIEnv* env, jobject obj, jboolean active);

JNIEXPORT jboolean JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSupportsRepeat(JNIEnv* env, jobject obj);
JNIEXPORT jint JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeGetRepeatMode(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSetRepeatMode(JNIEnv* env, jobject obj, jint mode);

JNIEXPORT jboolean JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSupportsSeek(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_dev_loki_lovisual_util_media_impl_win_WindowsMediaSession_nativeSeekTo(JNIEnv* env, jobject obj, jlong positionSeconds);

#ifdef __cplusplus
}
#endif
