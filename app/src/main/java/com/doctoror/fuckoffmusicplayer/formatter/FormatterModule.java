package com.doctoror.fuckoffmusicplayer.formatter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public final class FormatterModule {

    @Provides
    @Singleton
    ArtistAlbumFormatter provideArtistAlbumFormatter() {
        return new ArtistAlbumFormatter();
    }

}
