/*
 * Copyright (C) 2016 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.fuckoffmusicplayer.nowplaying;

import com.doctoror.fuckoffmusicplayer.R;
import com.doctoror.fuckoffmusicplayer.db.playlist.PlaylistProviderFiles;
import com.doctoror.fuckoffmusicplayer.di.DaggerHolder;
import com.doctoror.fuckoffmusicplayer.media.browser.SearchUtils;
import com.doctoror.fuckoffmusicplayer.playback.data.PlaybackData;
import com.doctoror.fuckoffmusicplayer.playlist.Media;
import com.doctoror.commons.util.Log;
import com.doctoror.fuckoffmusicplayer.playlist.PlaylistUtils;
import com.doctoror.fuckoffmusicplayer.util.ObserverAdapter;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * {@link NowPlayingActivity} intent handler
 */
public final class NowPlayingActivityIntentHandler {

    private static final String TAG = "IntentHandler";

    @Inject
    PlaybackData mPlaybackData;

    @Inject
    PlaylistProviderFiles mPlaylistProviderFiles;

    @NonNull
    private final Activity mActivity;

    NowPlayingActivityIntentHandler(@NonNull final Activity activity) {
        DaggerHolder.getInstance(activity).mainComponent().inject(this);
        mActivity = activity;
    }

    void handleIntent(@NonNull final Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            onActionView(mActivity, mPlaybackData, intent, mPlaylistProviderFiles);
        } else if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            onActionPlayFromSearch(mActivity, intent);
        }
    }

    private static void onActionView(@NonNull final Activity activity,
            @NonNull final PlaybackData playbackData,
            @NonNull final Intent intent,
            @NonNull final PlaylistProviderFiles playlistFactory) {
        rx.Observable.<List<Media>>create(s -> {
            try {
                s.onNext(playlistFromActionView(playlistFactory, intent));
            } catch (IOException e) {
                s.onError(e);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<List<Media>>() {
                    @Override
                    public void onError(final Throwable e) {
                        if (!activity.isFinishing()) {
                            Toast.makeText(activity.getApplicationContext(),
                                    R.string.Failed_to_start_playback, Toast.LENGTH_LONG)
                                    .show();
                        }
                    }

                    @Override
                    public void onNext(final List<Media> playlist) {
                        if (!activity.isFinishing()) {
                            if (playlist.isEmpty()) {
                                Toast.makeText(activity.getApplicationContext(),
                                        R.string.Failed_to_start_playback, Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                PlaylistUtils.play(activity, playbackData, playlist);
                            }
                        }
                    }
                });
    }

    private static void onActionPlayFromSearch(@NonNull final Activity activity,
            @NonNull final Intent intent) {
        rx.Observable.create(s ->
                new SearchUtils(activity).onPlayFromSearch(
                        intent.getStringExtra(SearchManager.QUERY), intent.getExtras()))
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @NonNull
    private static List<Media> playlistFromActionView(
            @NonNull final PlaylistProviderFiles playlistFactory,
            @NonNull final Intent intent) throws IOException {
        final Uri data = intent.getData();
        if (data == null) {
            Log.w(TAG, "Intent data is null");
            throw new IOException("Intent data is null");
        }

        final String scheme = data.getScheme();
        if (scheme == null) {
            Log.w(TAG, "Uri scheme is null");
            throw new IOException("Uri scheme is null");
        }
        switch (scheme) {
            case "file":
                return playlistFromFileActionView(playlistFactory, data);

            default:
                Log.w(TAG, "Unhandled Uri scheme: " + scheme);
                throw new IOException("Unhandled Uri scheme: " + scheme);
        }
    }

    @NonNull
    private static List<Media> playlistFromFileActionView(
            @NonNull final PlaylistProviderFiles playlistFactory,
            @NonNull final Uri data) throws IOException {
        try {
            return playlistFactory.fromFile(data);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}