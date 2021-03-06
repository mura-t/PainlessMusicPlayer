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
package com.doctoror.fuckoffmusicplayer.queue;

import android.databinding.ObservableField;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Yaroslav Mytkalyk on 20.10.16.
 */

public final class QueueActivityModel {

    private final ObservableField<RecyclerView.Adapter> mRecyclerAdpter = new ObservableField<>();
    private final ObservableField<String> mImageUri = new ObservableField<>();

    @NonNull
    public ObservableField<RecyclerView.Adapter> getRecyclerAdapter() {
        return mRecyclerAdpter;
    }

    public void setRecyclerAdpter(@Nullable final RecyclerView.Adapter<?> adapter) {
        mRecyclerAdpter.set(adapter);
    }

    @NonNull
    public ObservableField<String> getImageUri() {
        return mImageUri;
    }

    public void setImageUri(@Nullable final String imageUri) {
        mImageUri.set(imageUri);
    }
}
