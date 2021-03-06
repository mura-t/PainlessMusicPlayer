package com.doctoror.fuckoffmusicplayer.playback;

import com.doctoror.fuckoffmusicplayer.domain.playback.PlaybackServiceControl;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Playback service control implementation
 */
public final class PlaybackServiceControlImpl implements PlaybackServiceControl {
    
    private final Context context;

    public PlaybackServiceControlImpl(@NonNull final Context context) {
        this.context = context;
    }

    @Override
    public void resendState() {
        context.sendBroadcast(PlaybackServiceIntentFactory.intentResendState());
    }

    @Override
    public void playPause() {
        context.startService(PlaybackServiceIntentFactory.intentPlayPause(context));
    }

    @Override
    public void play() {
        context.startService(PlaybackServiceIntentFactory.intentPlay(context));
    }

    @Override
    public void playAnything() {
        context.startService(PlaybackServiceIntentFactory.intentPlayAnything(context));
    }

    @Override
    public void pause() {
        context.startService(PlaybackServiceIntentFactory.intentPause(context));
    }

    @Override
    public void stop() {
        context.startService(PlaybackServiceIntentFactory.intentStop(context));
    }

    @Override
    public void stopWithError(@NonNull final CharSequence errorMessage) {
        context.startService(PlaybackServiceIntentFactory.intentStopWithError(context, errorMessage));
    }

    @Override
    public void prev() {
        context.startService(PlaybackServiceIntentFactory.intentPrev(context));
    }

    @Override
    public void next() {
        context.startService(PlaybackServiceIntentFactory.intentNext(context));
    }

    @Override
    public void seek(final float positionPercent) {
        context.startService(PlaybackServiceIntentFactory.intentSeek(context, positionPercent));
    }

    @Override
    public void playMediaFromQueue(final long mediaId) {
        context.startService(PlaybackServiceIntentFactory.intentPlayMediaFromQueue(context, mediaId));
    }
}
