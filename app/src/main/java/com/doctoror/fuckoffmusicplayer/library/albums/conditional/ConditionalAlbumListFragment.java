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
package com.doctoror.fuckoffmusicplayer.library.albums.conditional;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.doctoror.commons.util.Log;
import com.doctoror.fuckoffmusicplayer.base.BaseActivity;
import com.doctoror.fuckoffmusicplayer.Henson;
import com.doctoror.fuckoffmusicplayer.R;
import com.doctoror.fuckoffmusicplayer.base.BaseFragment;
import com.doctoror.fuckoffmusicplayer.databinding.FragmentConditionalAlbumListBinding;
import com.doctoror.fuckoffmusicplayer.db.albums.AlbumsProvider;
import com.doctoror.fuckoffmusicplayer.db.queue.QueueProviderAlbums;
import com.doctoror.fuckoffmusicplayer.di.DaggerHolder;
import com.doctoror.fuckoffmusicplayer.nowplaying.NowPlayingActivity;
import com.doctoror.fuckoffmusicplayer.playback.data.PlaybackData;
import com.doctoror.fuckoffmusicplayer.queue.Media;
import com.doctoror.fuckoffmusicplayer.queue.QueueActivity;
import com.doctoror.fuckoffmusicplayer.queue.QueueUtils;
import com.doctoror.fuckoffmusicplayer.transition.CardVerticalGateTransition;
import com.doctoror.fuckoffmusicplayer.transition.TransitionUtils;
import com.doctoror.fuckoffmusicplayer.transition.VerticalGateTransition;
import com.doctoror.fuckoffmusicplayer.util.ObserverAdapter;
import com.doctoror.fuckoffmusicplayer.util.ViewUtils;
import com.doctoror.fuckoffmusicplayer.widget.DisableableAppBarLayout;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Album lists fragment
 */
public abstract class ConditionalAlbumListFragment extends BaseFragment {

    private static final String TAG = "ConditionalAlbumListFragment";

    private final ConditionalAlbumListModel mModel = new ConditionalAlbumListModel();

    private ConditionalAlbumsRecyclerAdapter mAdapter;

    private Subscription mOldSubscription;
    private Subscription mSubscription;

    private RequestManager mRequestManager;
    private Cursor mData;

    private Unbinder mUnbinder;

    @BindInt(R.integer.shortest_anim_time)
    int mAnimTime;

    @BindView(R.id.root)
    View root;

    @BindView(R.id.appBar)
    DisableableAppBarLayout appBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.albumArt)
    ImageView albumArt;

    @BindView(R.id.albumArtDim)
    View albumArtDim;

    @BindView(R.id.fab)
    View fab;

    @Nullable
    @BindView(R.id.cardHostScrollView)
    View cardHostScrollView;

    @Nullable
    @BindView(R.id.cardView)
    CardView cardView;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.progress)
    View progress;

    @BindView(R.id.errorContainer)
    View errorContainer;

    @BindView(R.id.emptyContainer)
    View emptyContainer;

    @Inject
    QueueProviderAlbums mQueueFactory;

    @Inject
    PlaybackData mPlaybackData;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerHolder.getInstance(getActivity()).mainComponent().inject(this);

        mRequestManager = Glide.with(this);

        mAdapter = new ConditionalAlbumsRecyclerAdapter(getActivity(), mRequestManager);
        mAdapter.setOnAlbumClickListener(this::onListItemClick);
        mModel.setRecyclerAdpter(mAdapter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        final FragmentConditionalAlbumListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_conditional_album_list, container, false);
        binding.setModel(mModel);

        final View view = binding.getRoot();
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public void onLayoutChildren(final RecyclerView.Recycler recycler,
                    final RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                setAppBarCollapsibleIfNeeded();
            }
        });

        if (TransitionUtils.supportsActivityTransitions()) {
            LollipopUtils.applyTransitions((BaseActivity) getActivity(), cardView != null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
    }

    private void setAppBarCollapsibleIfNeeded() {
        ViewUtils.setAppBarCollapsibleIfScrollableViewIsLargeEnoughToScroll(
                root, appBar, recyclerView, ViewUtils.getOverlayTop(cardHostScrollView != null
                        ? cardHostScrollView : recyclerView));
    }

    @Override
    public void onStart() {
        super.onStart();
        fab.setScaleX(1f);
        fab.setScaleY(1f);
        albumArtDim.setAlpha(1f);
        restartLoader();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.changeCursor(null);
    }

    @NonNull
    protected Observable<List<Media>> queueFromAlbum(final long albumId) {
        return mQueueFactory.fromAlbum(albumId);
    }

    @NonNull
    protected Observable<List<Media>> queueFromAlbums(@NonNull final long[] albumIds) {
        return mQueueFactory.fromAlbums(albumIds, null);
    }

    private void onListItemClick(final int position,
            final long albumId,
            @Nullable final String queueName) {
        registerOnStartSubscription(queueFromAlbum(albumId)
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<List<Media>>() {
                    @Override
                    public void onNext(final List<Media> queue) {
                        if (isAdded()) {
                            onQueueLoaded(queue, ViewUtils.getItemView(recyclerView, position),
                                    queueName);
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {
                        if (isAdded()) {
                            onQueueEmpty();
                        }
                    }
                }));
    }

    private void onQueueLoaded(@NonNull final List<Media> queue,
            @Nullable final View itemView,
            @Nullable final String queueName) {
        final Activity activity = getActivity();
        if (queue.isEmpty()) {
            onQueueEmpty();
        } else {
            final Intent intent = Henson.with(activity).gotoQueueActivity()
                    .hasCoverTransition(false)
                    .hasItemViewTransition(false)
                    .isNowPlayingQueue(false)
                    .queue(queue)
                    .title(queueName)
                    .build();

            Bundle options = null;
            if (itemView != null) {
                options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, itemView,
                        QueueActivity.TRANSITION_NAME_ROOT).toBundle();
            }

            startActivity(intent, options);
        }
    }

    private void onQueueEmpty() {
        Toast.makeText(getActivity(), R.string.The_queue_is_empty, Toast.LENGTH_SHORT).show();
    }

    private void onPlayClick(@NonNull final long[] albumIds) {
        queueFromAlbums(albumIds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<List<Media>>() {
                    @Override
                    public void onNext(final List<Media> queue) {
                        if (isAdded()) {
                            if (queue.isEmpty()) {
                                onQueueEmpty();
                            } else {
                                QueueUtils.play(getActivity(), mPlaybackData, queue);
                                prepareViewsAndExit(() -> NowPlayingActivity.start(getActivity(),
                                        albumArt, null), true);
                            }
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {
                        onQueueEmpty();
                    }
                });
    }

    private void prepareViewsAndExit(@NonNull final Runnable exitAction,
            final boolean fadeDim) {
        if (!TransitionUtils.supportsActivityTransitions() || fab.getScaleX() == 0f) {
            exitAction.run();
        } else {
            if (fadeDim) {
                albumArtDim.animate().alpha(0f).setDuration(mAnimTime).start();
            }
            fab.animate().scaleX(0f).scaleY(0f).setDuration(mAnimTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(final Animator animation) {
                            exitAction.run();
                        }
                    }).start();
        }
    }

    private void restartLoader() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mOldSubscription = mSubscription;
            mSubscription = registerOnStartSubscription(load()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mObserver));
        } else {
            Log.w(TAG, "restartLoader is called, READ_EXTERNAL_STORAGE is not granted");
        }
    }

    protected abstract Observable<Cursor> load();

    private void showStateError() {
        fab.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        if (cardView == null) {
            // Collapse for non-card-view
            appBar.setExpanded(false, false);
        } else {
            appBar.setExpanded(true, false);
        }
        setAppBarCollapsibleIfNeeded();
    }

    private void showStateEmpty() {
        fab.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        if (cardView == null) {
            // Collapse for non-card-view
            appBar.setExpanded(false, false);
        } else {
            appBar.setExpanded(true, false);
        }
        setAppBarCollapsibleIfNeeded();
    }

    private void showStateContent() {
        fab.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        setAppBarCollapsibleIfNeeded();
    }

    @OnClick(R.id.fab)
    public void onFabClick() {
        if (mData != null) {
            final long[] ids = new long[mData.getCount()];
            int i = 0;
            for (mData.moveToFirst(); !mData.isAfterLast(); mData.moveToNext(), i++) {
                ids[i] = mData.getLong(AlbumsProvider.COLUMN_ID);
            }
            onPlayClick(ids);
        }
    }

    private void loadAlbumArt(@NonNull final Cursor cursor) {
        if (albumArt != null) {
            final String pic = findAlbumArt(cursor);
            if (TextUtils.isEmpty(pic)) {
                showPlaceholderAlbumArt();
            } else {
                mRequestManager.load(pic)
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(final Exception e, final String model,
                                    final Target<GlideDrawable> target,
                                    final boolean isFirstResource) {
                                showPlaceholderAlbumArt();
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(final GlideDrawable resource,
                                    final String model,
                                    final Target<GlideDrawable> target,
                                    final boolean isFromMemoryCache,
                                    final boolean isFirstResource) {
                                ((AppCompatActivity) getActivity())
                                        .supportStartPostponedEnterTransition();
                                return false;
                            }
                        })
                        .into(albumArt);
            }
        }
    }

    private void showPlaceholderAlbumArt() {
        Glide.clear(albumArt);
        albumArt.setImageResource(R.drawable.album_art_placeholder);
        albumArt.setAlpha(1f);
        ((AppCompatActivity) getActivity()).supportStartPostponedEnterTransition();
    }

    @Nullable
    private String findAlbumArt(@NonNull final Cursor cursor) {
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            final String art = cursor.getString(AlbumsProvider.COLUMN_ALBUM_ART);
            if (!TextUtils.isEmpty(art)) {
                return art;
            }
        }
        return null;
    }

    private final Observer<Cursor> mObserver = new ObserverAdapter<Cursor>() {

        @Override
        public void onError(final Throwable e) {
            if (mOldSubscription != null) {
                mOldSubscription.unsubscribe();
                mOldSubscription = null;
            }
            if (mData != null) {
                mData.close();
                mData = null;
            }
            if (isAdded()) {
                showStateError();
            }
        }

        @Override
        public void onNext(final Cursor cursor) {
            loadAlbumArt(cursor);
            mAdapter.changeCursor(cursor);
            mData = cursor;

            if (cursor.getCount() == 0) {
                showStateEmpty();
            } else {
                showStateContent();
            }

            if (mOldSubscription != null) {
                mOldSubscription.unsubscribe();
                mOldSubscription = null;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static final class LollipopUtils {

        static void applyTransitions(@NonNull final BaseActivity activity,
                final boolean hasCardView) {
            TransitionUtils.clearSharedElementsOnReturn(activity);
            activity.getWindow().setReturnTransition(hasCardView
                    ? new CardVerticalGateTransition()
                    : new VerticalGateTransition());
        }
    }
}
